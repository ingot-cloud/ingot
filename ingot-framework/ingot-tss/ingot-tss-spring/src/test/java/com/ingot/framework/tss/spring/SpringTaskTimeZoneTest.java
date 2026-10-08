package com.ingot.framework.tss.spring;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

import com.ingot.framework.tss.common.model.TaskDefinition;
import com.ingot.framework.tss.common.registry.DefaultTaskRegistry;
import com.ingot.framework.tss.spring.config.SpringTaskAutoConfiguration;
import com.ingot.framework.tss.spring.management.SpringTaskManagement;
import com.ingot.framework.tss.spring.properties.SpringTaskProperties;
import com.ingot.framework.tss.spring.scheduler.SpringTaskScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>注册与更新 Cron 使用明确的业务时区，不依赖 JVM 默认值。</p>
 * @author jy
 * @since 1.0.0
 */
class SpringTaskTimeZoneTest {
    @Test
    void registrationAndCronUpdatesStayOnShanghaiAcrossJvmZones() {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var engine = mock(org.springframework.scheduling.TaskScheduler.class);
                doReturn(mock(ScheduledFuture.class)).when(engine).schedule(any(Runnable.class), any(Trigger.class));
                var registry = new DefaultTaskRegistry();
                var scheduler = new SpringTaskScheduler(registry, engine, new SpringTaskProperties());
                scheduler.registerTask(TaskDefinition.builder().taskName("daily").cron("0 0 9 * * *").build());
                assertTrue(new SpringTaskManagement(scheduler, registry).updateTaskCron("daily", "0 30 9 * * *"));
                var triggers = ArgumentCaptor.forClass(Trigger.class);
                verify(engine, times(2)).schedule(any(Runnable.class), triggers.capture());
                Clock clock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC);
                assertEquals(Instant.parse("2026-10-08T01:00:00Z"), triggers.getAllValues().get(0).nextExecution(new SimpleTriggerContext(clock)));
                assertEquals(Instant.parse("2026-10-08T01:30:00Z"), triggers.getAllValues().get(1).nextExecution(new SimpleTriggerContext(clock)));
                scheduler.destroy();
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void invalidZoneFailsStartupAndDefaultIsShanghai() {
        var runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                SpringTaskAutoConfiguration.class, ValidationAutoConfiguration.class));
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("Asia/Shanghai", context.getBean(SpringTaskProperties.class).getTimeZone().getId());
        });
        runner.withPropertyValues("ingot.tss.spring.time-zone=Invalid/TimeZone").run(context -> assertNotNull(context.getStartupFailure()));
    }
}
