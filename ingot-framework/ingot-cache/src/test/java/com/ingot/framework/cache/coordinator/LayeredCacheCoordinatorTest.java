package com.ingot.framework.cache.coordinator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.InvalidationEvent;
import com.ingot.framework.eventbus.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>{@link LayeredCacheCoordinator} 的分域派发、全量派发与回调隔离行为验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LayeredCacheCoordinatorTest {

    private static final String ALL = "ALL";

    static class TestEvent extends InvalidationEvent {
        private final String domain;

        TestEvent(String domain) {
            this.domain = domain;
        }

        String getDomain() {
            return domain;
        }
    }

    private InvalidationBus bus;
    private Subscription subscription;
    private Consumer<TestEvent> handler;
    private LayeredCacheCoordinator<TestEvent, String> coordinator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        bus = mock(InvalidationBus.class);
        subscription = mock(Subscription.class);
        when(bus.subscribe(eq(TestEvent.class), any())).thenAnswer(inv -> {
            handler = inv.getArgument(1);
            return subscription;
        });
        coordinator = new LayeredCacheCoordinator<>(bus, TestEvent.class, TestEvent::getDomain, ALL);
        coordinator.start();
    }

    @Test
    @DisplayName("按域派发，只触发匹配域的回调")
    void dispatchesByDomain() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> fired.add("a"));
        coordinator.register("B", () -> fired.add("b"));

        handler.accept(new TestEvent("A"));

        assertThat(fired).containsExactly("a");
    }

    @Test
    @DisplayName("同一域多个回调按注册顺序串行执行")
    void runsMultipleEvictorsInOrder() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> fired.add("first"));
        coordinator.register("A", () -> fired.add("second"));

        handler.accept(new TestEvent("A"));

        assertThat(fired).containsExactly("first", "second");
    }

    @Test
    @DisplayName("全量域事件触发所有回调")
    void allDomainTriggersEverything() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> fired.add("a"));
        coordinator.register("B", () -> fired.add("b"));

        handler.accept(new TestEvent(ALL));

        assertThat(fired).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("域为空时按全量处理")
    void nullDomainTriggersEverything() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> fired.add("a"));
        coordinator.register("B", () -> fired.add("b"));

        handler.accept(new TestEvent(null));

        assertThat(fired).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("单个回调异常不影响同域其余回调")
    void isolatesFailingEvictor() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> {
            throw new IllegalStateException("boom");
        });
        coordinator.register("A", () -> fired.add("survived"));

        handler.accept(new TestEvent("A"));

        assertThat(fired).containsExactly("survived");
    }

    @Test
    @DisplayName("单个域异常不影响全量派发中的其他域")
    void isolatesFailingDomain() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> {
            throw new IllegalStateException("boom");
        });
        coordinator.register("B", () -> fired.add("b"));

        handler.accept(new TestEvent(ALL));

        assertThat(fired).containsExactly("b");
    }

    @Test
    @DisplayName("未注册的域不产生任何动作")
    void unknownDomainIsNoOp() {
        List<String> fired = new ArrayList<>();
        coordinator.register("A", () -> fired.add("a"));

        handler.accept(new TestEvent("UNKNOWN"));

        assertThat(fired).isEmpty();
    }

    @Test
    @DisplayName("停止时取消订阅且可重复调用")
    void stopClosesSubscription() {
        coordinator.stop();
        coordinator.stop();

        verify(subscription).close();
    }
}
