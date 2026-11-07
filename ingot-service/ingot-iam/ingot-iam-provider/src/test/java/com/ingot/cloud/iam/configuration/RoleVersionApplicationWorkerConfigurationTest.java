package com.ingot.cloud.iam.configuration;

import com.ingot.cloud.iam.infrastructure.roleapplication.RoleVersionApplicationWorkerProperties;

import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.cloud.iam.application.roleapplication.PlatformRoleVersionApplicationAdapter;
import com.ingot.cloud.iam.application.roleapplication.RoleVersionApplicationPort;
import com.ingot.cloud.iam.application.roleapplication.RoleVersionApplicationService;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationReadyEvent;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationWork;
import com.ingot.cloud.iam.infrastructure.roleapplication.RoleVersionApplicationScheduler;
import com.ingot.cloud.iam.infrastructure.roleapplication.RoleVersionApplicationWakeupNotifier;

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
class RoleVersionApplicationWorkerConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RoleVersionApplicationWorkerConfiguration.class)
            .withBean(RoleVersionApplicationWork.class, () -> {
                var service = mock(RoleVersionApplicationWork.class);
                when(service.discover(any(), anyInt())).thenReturn(List.of());
                when(service.executeTask(any())).thenReturn(RoleVersionApplicationWork.BatchResult.STOP);
                return service;
            });

    @Test
    void defaultsCreateIndependentWorkerAndNotifierWithoutRedis() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBean(RoleVersionApplicationScheduler.class).isRunning());
            assertTrue(context.getBean(RoleVersionApplicationWakeupNotifier.class).isRunning());
            var properties = context.getBean(RoleVersionApplicationWorkerProperties.class);
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
                    assertTrue(context.getBeansOfType(RoleVersionApplicationScheduler.class).isEmpty());
                    context.publishEvent(new RoleVersionApplicationReadyEvent(java.math.BigInteger.ONE, java.math.BigInteger.TWO));
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
        var fractional = new RoleVersionApplicationWorkerProperties();
        fractional.setActiveBatchDelay(Duration.ofNanos(1));
        assertDoesNotThrow(fractional::validate);
    }

    @Test
    void springTssDoesNotRegisterWorkerOrSupplyItsExecutionThreads() {
        runner.withConfiguration(AutoConfigurations.of(SpringTaskAutoConfiguration.class)).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(RoleVersionApplicationScheduler.class).size());
            assertTrue(context.getBean(TaskRegistry.class).getAllTaskNames().isEmpty());
            assertTrue(context.getBean(com.ingot.framework.tss.common.scheduler.TaskScheduler.class).getAllTasks().isEmpty());
            var tssPool = (ThreadPoolTaskScheduler) context.getBean(org.springframework.scheduling.TaskScheduler.class);
            tssPool.shutdown();
            var executed = new CountDownLatch(1);
            when(context.getBean(RoleVersionApplicationWork.class).executeTask(java.math.BigInteger.ONE)).thenAnswer(invocation -> {
                assertTrue(Thread.currentThread().getName().startsWith("iam-role-application-worker"));
                executed.countDown(); return RoleVersionApplicationWork.BatchResult.STOP;
            });
            context.getBean(RoleVersionApplicationScheduler.class).wake(java.math.BigInteger.ONE);
            try { assertTrue(executed.await(2, TimeUnit.SECONDS)); }
            catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
        });
    }

    @Test
    void tssTypeDoesNotSelectOrDisableRoleWorker() {
        var withTss = runner.withConfiguration(AutoConfigurations.of(SpringTaskAutoConfiguration.class));
        withTss.withPropertyValues("ingot.tss.type=xxljob").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBean(RoleVersionApplicationScheduler.class).isRunning());
            assertTrue(context.getBeansOfType(TaskRegistry.class).isEmpty());
        });
        withTss.withPropertyValues("ingot.tss.type=spring", "ingot.iam.role-applications.worker-enabled=false").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(RoleVersionApplicationScheduler.class).isEmpty());
            assertNotNull(context.getBean(TaskRegistry.class));
            assertTrue(context.getBean(RoleVersionApplicationWakeupNotifier.class).isRunning());
        });
    }

    @Test
    void productionDomainAdapterAndServiceWireAsSingleWorkImplementation() {
        new ApplicationContextRunner().withUserConfiguration(RoleVersionApplicationWorkerConfiguration.class,
                        RoleVersionApplicationService.class, PlatformRoleVersionApplicationAdapter.class)
                .withBean(com.ingot.cloud.iam.application.authorization.IamAccess.class, () -> mock(com.ingot.cloud.iam.application.authorization.IamAccess.class))
                .withBean(PlatformRoleFacade.class, () -> mock(PlatformRoleFacade.class))
                .withBean(com.ingot.cloud.iam.infrastructure.persistence.RoleRepository.class, () -> mock(com.ingot.cloud.iam.infrastructure.persistence.RoleRepository.class))
                .withBean(com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository.class, () -> mock(com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository.class))
                .withBean(com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade.class, () -> mock(com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade.class))
                .withBean(com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService.class, () -> mock(com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService.class))
                .withBean(com.ingot.cloud.iam.application.identity.ActiveIdentityService.class, () -> mock(com.ingot.cloud.iam.application.identity.ActiveIdentityService.class))
                .withBean(com.ingot.cloud.iam.application.authorization.IamActionAuthorizer.class, () -> mock(com.ingot.cloud.iam.application.authorization.IamActionAuthorizer.class))
                .withBean(com.ingot.cloud.iam.infrastructure.persistence.RoleVersionApplicationRepository.class, () -> mock(com.ingot.cloud.iam.infrastructure.persistence.RoleVersionApplicationRepository.class))
                .withBean(com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter.class, () -> mock(com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter.class))
                .withBean(org.springframework.transaction.PlatformTransactionManager.class, () -> mock(org.springframework.transaction.PlatformTransactionManager.class))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(RoleVersionApplicationPort.class).size());
                    assertSame(context.getBean(RoleVersionApplicationService.class), context.getBean(RoleVersionApplicationWork.class));
                    assertTrue(context.getBean(RoleVersionApplicationScheduler.class).isRunning());
                });
    }
}
