package com.ingot.cloud.iam.role;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.eventbus.InvalidationBus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>装配独立调度与发送线程，任务按ID唤醒，数据库低频扫描仅负责恢复遗漏。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RoleApplicationWorkerProperties.class)
public class RoleApplicationWorkerConfiguration {
    private static final String TIMER_THREAD = "iam-role-application-timer";
    private static final String EXECUTION_THREAD = "iam-role-application-worker";
    private static final String SCAN_THREAD = "iam-role-application-discovery";
    private static final String SENDER_THREAD = "iam-role-application-notification";
    private static final String SUBSCRIPTION_THREAD = "iam-role-application-subscription";

    /** 装配已有Actuator注册表指标，未提供注册表时使用本地计数器。 */
    @Bean
    public RoleApplicationWorkerMetrics roleApplicationWorkerMetrics(ObjectProvider<MeterRegistry> registries) {
        return new RoleApplicationWorkerMetrics(registries.getIfAvailable(SimpleMeterRegistry::new));
    }

    /**
     * 装配默认开启的领取器，线程资源由生命周期管理；HTTP身份不进入调度器。
     */
    @Bean
    @ConditionalOnProperty(prefix = RoleApplicationWorkerProperties.PREFIX,
            name = RoleApplicationWorkerProperties.WORKER_ENABLED,
            havingValue = RoleApplicationWorkerProperties.ENABLED_VALUE, matchIfMissing = true)
    public RoleApplicationScheduler roleApplicationWorker(RoleApplicationWork applications,
            RoleApplicationWorkerProperties properties, RoleApplicationWorkerMetrics metrics) {
        return new RoleApplicationScheduler(applications, properties, timer(TIMER_THREAD),
                Executors.newSingleThreadExecutor(threads(EXECUTION_THREAD)),
                Executors.newSingleThreadExecutor(threads(SCAN_THREAD)), System::nanoTime,
                bound -> ThreadLocalRandom.current().nextLong(bound), metrics);
    }

    /** 关闭本节点领取后仍能广播；总线是优化依赖，缺省时本地及数据库恢复有效。 */
    @Bean
    public RoleApplicationWakeupNotifier roleApplicationWakeupNotifier(RoleApplicationWorkerProperties properties,
            ObjectProvider<RoleApplicationScheduler> workers, ObjectProvider<InvalidationBus> buses,
            RoleApplicationWorkerMetrics metrics) {
        var sender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.NANOSECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()), threads(SENDER_THREAD),
                new ThreadPoolExecutor.AbortPolicy());
        return new RoleApplicationWakeupNotifier(workers::getIfAvailable, buses::getIfAvailable,
                sender, timer(SUBSCRIPTION_THREAD), metrics);
    }

    private static ScheduledThreadPoolExecutor timer(String name) {
        var executor = new ScheduledThreadPoolExecutor(1, threads(name));
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private static ThreadFactory threads(String name) {
        return operation -> {
            var thread = new Thread(operation, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
