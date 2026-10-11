package com.ingot.cloud.iam.role;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.tss.common.registry.TaskRegistry;
import com.ingot.framework.tss.spring.config.SpringTaskAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
            .withBean(RoleApplicationWork.class, () -> {
                var service = mock(RoleApplicationWork.class);
                when(service.discover(any(), anyInt())).thenReturn(List.of());
                when(service.executeTask(any())).thenReturn(RoleApplicationWork.BatchResult.STOP);
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

    @Test
    void springTssDoesNotRegisterWorkerOrSupplyItsExecutionThreads() {
        runner.withConfiguration(AutoConfigurations.of(SpringTaskAutoConfiguration.class)).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(RoleApplicationScheduler.class).size());
            assertTrue(context.getBean(TaskRegistry.class).getAllTaskNames().isEmpty());
            assertTrue(context.getBean(com.ingot.framework.tss.common.scheduler.TaskScheduler.class).getAllTasks().isEmpty());
            var tssPool = (ThreadPoolTaskScheduler) context.getBean(org.springframework.scheduling.TaskScheduler.class);
            tssPool.shutdown();
            var executed = new CountDownLatch(1);
            when(context.getBean(RoleApplicationWork.class).executeTask(java.math.BigInteger.ONE)).thenAnswer(invocation -> {
                assertTrue(Thread.currentThread().getName().startsWith("iam-role-application-worker"));
                executed.countDown(); return RoleApplicationWork.BatchResult.STOP;
            });
            context.getBean(RoleApplicationScheduler.class).wake(java.math.BigInteger.ONE);
            try { assertTrue(executed.await(2, TimeUnit.SECONDS)); }
            catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
        });
    }

    @Test
    void tssTypeDoesNotSelectOrDisableRoleWorker() {
        var withTss = runner.withConfiguration(AutoConfigurations.of(SpringTaskAutoConfiguration.class));
        withTss.withPropertyValues("ingot.tss.type=xxljob").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBean(RoleApplicationScheduler.class).isRunning());
            assertTrue(context.getBeansOfType(TaskRegistry.class).isEmpty());
        });
        withTss.withPropertyValues("ingot.tss.type=spring", "ingot.iam.role-applications.worker-enabled=false").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(RoleApplicationScheduler.class).isEmpty());
            assertNotNull(context.getBean(TaskRegistry.class));
            assertTrue(context.getBean(RoleApplicationWakeupNotifier.class).isRunning());
        });
    }

    @Test
    void productionDomainAdapterAndServiceWireAsSingleWorkImplementation() {
        new ApplicationContextRunner().withUserConfiguration(RoleApplicationWorkerConfiguration.class,
                        RoleApplicationService.class, PlatformRoleApplicationDomainSupport.class)
                .withBean(com.ingot.cloud.iam.support.IamAccess.class, () -> mock(com.ingot.cloud.iam.support.IamAccess.class))
                .withBean(RoleService.class, () -> mock(RoleService.class))
                .withBean(com.ingot.cloud.iam.persistence.RoleRepository.class, () -> mock(com.ingot.cloud.iam.persistence.RoleRepository.class))
                .withBean(com.ingot.cloud.iam.persistence.AssignmentRepository.class, () -> mock(com.ingot.cloud.iam.persistence.AssignmentRepository.class))
                .withBean(com.ingot.cloud.iam.assignment.AssignmentService.class, () -> mock(com.ingot.cloud.iam.assignment.AssignmentService.class))
                .withBean(com.ingot.cloud.iam.evaluation.ResourceAccess.class, () -> mock(com.ingot.cloud.iam.evaluation.ResourceAccess.class))
                .withBean(com.ingot.cloud.iam.identity.ActiveIdentityService.class, () -> mock(com.ingot.cloud.iam.identity.ActiveIdentityService.class))
                .withBean(com.ingot.cloud.iam.authorization.IamActionAuthorizer.class, () -> mock(com.ingot.cloud.iam.authorization.IamActionAuthorizer.class))
                .withBean(com.ingot.cloud.iam.persistence.RoleApplicationRepository.class, () -> mock(com.ingot.cloud.iam.persistence.RoleApplicationRepository.class))
                .withBean(com.ingot.cloud.iam.support.IamAuditWriter.class, () -> mock(com.ingot.cloud.iam.support.IamAuditWriter.class))
                .withBean(org.springframework.transaction.PlatformTransactionManager.class, () -> mock(org.springframework.transaction.PlatformTransactionManager.class))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(RoleApplicationDomainSupport.class).size());
                    assertSame(context.getBean(RoleApplicationService.class), context.getBean(RoleApplicationWork.class));
                    assertTrue(context.getBean(RoleApplicationScheduler.class).isRunning());
                });
    }
}
