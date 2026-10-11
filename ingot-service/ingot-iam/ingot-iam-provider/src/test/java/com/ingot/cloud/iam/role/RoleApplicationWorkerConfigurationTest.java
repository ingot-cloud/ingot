package com.ingot.cloud.iam.role;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.eventbus.InvalidationBus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证配置绑定、启动门禁和真实Spring事件接线，不启动任何业务连接。</p>
 * @author jy
 * @since 1.0.0
 */
class RoleApplicationWorkerConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RoleApplicationWorkerConfiguration.class)
            .withBean(RoleApplicationService.class, () -> {
                var service = mock(RoleApplicationService.class);
                when(service.discover(any(), anyInt())).thenReturn(List.of());
                return service;
            });

    @Test
    void defaultsCreateIndependentWorkerAndNotifierWithoutRedis() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBean(RoleApplicationScheduler.class).isRunning());
            assertTrue(context.getBean(RoleApplicationWakeupNotifier.class).isRunning());
            var properties = context.getBean(RoleApplicationWorkerProperties.class);
            assertEquals(Duration.ofSeconds(60), properties.getFallbackScanInterval());
            assertEquals(Duration.ofMillis(250), properties.getActiveBatchDelay());
            assertEquals(256, properties.getQueueCapacity());
        });
    }

    @Test
    void disabledWorkerStillReceivesCommittedEventsAndBroadcastsAsynchronously() {
        var published = new CountDownLatch(1);
        var bus = mock(InvalidationBus.class);
        doAnswer(invocation -> { published.countDown(); return null; }).when(bus).publish(any());
        runner.withPropertyValues("ingot.iam.role-applications.worker-enabled=false")
                .withBean(InvalidationBus.class, () -> bus).run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(RoleApplicationScheduler.class).isEmpty());
                    context.publishEvent(new RoleApplicationReadyEvent(java.math.BigInteger.ONE, java.math.BigInteger.TWO));
                    try { assertTrue(published.await(2, TimeUnit.SECONDS)); }
                    catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
                });
    }

    @Test
    void invalidIntervalsAndCapacityFailStartup() {
        for (var property : List.of("fallback-scan-interval=9s", "active-batch-delay=0ms",
                "active-batch-delay=10s", "queue-capacity=0")) {
            runner.withPropertyValues("ingot.iam.role-applications." + property).run(context ->
                    assertNotNull(context.getStartupFailure(), property));
        }
        var fractional = new RoleApplicationWorkerProperties();
        fractional.setActiveBatchDelay(Duration.ofNanos(1));
        assertDoesNotThrow(fractional::validate);
    }
}
