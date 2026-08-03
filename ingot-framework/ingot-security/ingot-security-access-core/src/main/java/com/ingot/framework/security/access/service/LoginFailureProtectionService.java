package com.ingot.framework.security.access.service;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.cloud.security.api.model.enums.SecurityEventCategory;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.framework.security.access.model.LoginFailureContext;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 登录失败保护执行服务。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class LoginFailureProtectionService {

    private static final String COUNTER_PREFIX = RedisKeyConstants.LoginFailure.COUNTER_PREFIX;
    private static final String RULE_CODE = "login-failure";

    private final LoginFailurePolicyLoader policyLoader;
    private final LoginFailureCounter counter;
    private final TempBlockWriter tempBlockWriter;
    private final Consumer<SecurityEventReportDTO> eventReporter;

    public void recordFailure(LoginFailureContext ctx) {
        if (ctx == null) {
            return;
        }
        List<LoginFailurePolicy> policies = policyLoader.loadAll();
        for (LoginFailurePolicy policy : policies) {
            if (!policy.enabled()) {
                continue;
            }
            handleFailure(ctx, policy);
        }
    }

    public void recordSuccess(LoginFailureContext ctx) {
        if (ctx == null) {
            return;
        }
        List<LoginFailurePolicy> policies = policyLoader.loadAll();
        for (LoginFailurePolicy policy : policies) {
            if (!policy.enabled()) {
                continue;
            }
            String counterKey = buildCounterKey(ctx, policy.dimension());
            if (counterKey != null) {
                counter.reset(counterKey);
            }
        }
    }

    private void handleFailure(LoginFailureContext ctx, LoginFailurePolicy policy) {
        String counterKey = buildCounterKey(ctx, policy.dimension());
        if (counterKey == null) {
            return;
        }
        Duration window = Duration.ofMinutes(policy.windowMinutes());
        long count = counter.increment(counterKey, window);
        if (count < policy.maxAttempts()) {
            return;
        }
        String blockValue = buildBlockValue(ctx, policy.dimension());
        if (blockValue == null) {
            return;
        }
        tempBlockWriter.block(policy.blockKeyType(), blockValue, Duration.ofSeconds(policy.blockTtlSec()));
        reportEvent(ctx, policy, count);
        log.warn("[LoginFailure] dimension={} count={} blocked keyType={} value={}",
                policy.dimension(), count, policy.blockKeyType(), blockValue);
    }

    private void reportEvent(LoginFailureContext ctx, LoginFailurePolicy policy, long count) {
        if (eventReporter == null) {
            return;
        }
        SecurityEventType type = eventTypeFor(policy.dimension());
        if (type == null) {
            return;
        }
        Map<String, Object> extension = new HashMap<>(4);
        extension.put("dimension", policy.dimension().name());
        extension.put("countInWindow", count);
        extension.put("ttlSec", policy.blockTtlSec());
        if (StrUtil.isNotBlank(ctx.username())) {
            extension.put("username", ctx.username());
        }
        SecurityEventReportDTO dto = SecurityEventReportDTO.builder()
                .eventType(type.getCode())
                .eventCategory(SecurityEventCategory.ACCESS.getCode())
                .occurredAt(LocalDateTime.now())
                .account(ctx.username())
                .userType(ctx.userType())
                .clientId(ctx.clientId())
                .deviceId(ctx.deviceId())
                .clientIp(ctx.ip())
                .result("BLOCKED")
                .reasonCode(RULE_CODE)
                .sourceModule("ingot-auth")
                .extension(extension)
                .build();
        try {
            eventReporter.accept(dto);
        } catch (Exception e) {
            log.warn("[LoginFailure] report event failed dimension={}", policy.dimension(), e);
        }
    }

    static String buildCounterKey(LoginFailureContext ctx, LoginFailureDimension dimension) {
        return switch (dimension) {
            case IP -> blankToNull(ctx.ip()) == null ? null : COUNTER_PREFIX + "ip:" + ctx.ip();
            case DEVICE -> blankToNull(ctx.deviceId()) == null ? null : COUNTER_PREFIX + "dv:" + ctx.deviceId();
            case CLIENT -> blankToNull(ctx.clientId()) == null ? null : COUNTER_PREFIX + "cl:" + ctx.clientId();
            case ACCOUNT_IP -> {
                if (blankToNull(ctx.username()) == null || blankToNull(ctx.ip()) == null) {
                    yield null;
                }
                String userType = ctx.userType() == null ? "" : ctx.userType();
                yield COUNTER_PREFIX + "ui-ip:" + userType + ":" + ctx.username() + ":" + ctx.ip();
            }
        };
    }

    static String buildBlockValue(LoginFailureContext ctx, LoginFailureDimension dimension) {
        return switch (dimension) {
            case IP, ACCOUNT_IP -> ctx.ip();
            case DEVICE -> ctx.deviceId();
            case CLIENT -> ctx.clientId();
        };
    }

    private static SecurityEventType eventTypeFor(LoginFailureDimension dimension) {
        return switch (dimension) {
            case IP -> SecurityEventType.LOGIN_FAIL_IP_EXCEED;
            case DEVICE -> SecurityEventType.LOGIN_FAIL_DEVICE_EXCEED;
            case CLIENT -> SecurityEventType.LOGIN_FAIL_CLIENT_EXCEED;
            case ACCOUNT_IP -> SecurityEventType.LOGIN_FAIL_ACCOUNT_IP_EXCEED;
        };
    }

    private static String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
