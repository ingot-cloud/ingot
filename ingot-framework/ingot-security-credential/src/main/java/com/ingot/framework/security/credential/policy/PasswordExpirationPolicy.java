package com.ingot.framework.security.credential.policy;

import com.ingot.framework.security.credential.exception.PasswordExpiredException;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * 密码过期策略
 * <p>适用场景：登录</p>
 *
 * @author jymot
 * @since 2026-01-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PasswordExpirationPolicy implements PasswordPolicy {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 密码最大有效天数（0表示永不过期）
     */
    private int maxDays = 90;

    /**
     * 提前N天开始提醒
     */
    private int warningDaysBefore = 7;

    /**
     * 宽限期登录次数（过期后还可以登录的次数）
     */
    private int graceLoginCount = 3;

    /**
     * 强制修改密码后是否立即过期
     */
    private boolean forceChangeAfterReset = true;

    /**
     * 优先级（可覆盖）
     */
    private int priority = 20;

    @Override
    public String getName() {
        return "EXPIRATION";
    }

    @Override
    public int getPriority() {
        return priority; // 使用可配置的优先级
    }

    @Override
    public boolean isBlocking() {
        return false; // 不是阻断式策略，允许宽限期登录
    }

    @Override
    public Set<CredentialScene> getApplicableScenes() {
        // 只适用于登录场景
        return Set.of(CredentialScene.LOGIN);
    }

    @Override
    public PasswordCheckResult check(PolicyCheckContext context) {
        if (!enabled || maxDays <= 0) {
            return PasswordCheckResult.pass();
        }

        // 获取密码修改时间
        LocalDateTime lastChangedAt = context.getLastPasswordChangedAt();
        if (lastChangedAt == null) {
            // 没有记录，假设刚设置
            return PasswordCheckResult.pass();
        }

        // 计算过期时间
        LocalDateTime expiresAt = lastChangedAt.plusDays(maxDays);
        LocalDateTime now = LocalDateTime.now();

        // 检查是否过期
        if (now.isAfter(expiresAt)) {
            // 已过期
            Integer graceRemaining = context.getGraceLoginRemaining();
            if (graceRemaining != null && graceRemaining > 0) {
                // 还有宽限期登录次数
                return PasswordCheckResult.warning(
                    String.format("密码已过期，剩余%d次宽限登录机会", graceRemaining),
                    "EXPIRED_WITH_GRACE"
                );
            } else {
                // 宽限期已用完
                return PasswordCheckResult.fail(
                    "密码已过期，请立即修改密码",
                    new PasswordExpiredException("密码已过期")
                );
            }
        }

        // 检查是否需要提醒
        LocalDateTime warningStartAt = expiresAt.minusDays(warningDaysBefore);
        if (now.isAfter(warningStartAt)) {
            long daysLeft = ChronoUnit.DAYS.between(now, expiresAt);
            return PasswordCheckResult.warning(
                String.format("密码将在%d天后过期，请及时修改", daysLeft),
                "EXPIRING_SOON"
            );
        }

        // 检查是否强制修改
        Boolean forceChange = context.getForcePasswordChange();
        if (Boolean.TRUE.equals(forceChange)) {
            return PasswordCheckResult.fail(
                "管理员已重置您的密码，请立即修改",
                new PasswordExpiredException("强制修改密码")
            );
        }

        return PasswordCheckResult.pass();
    }
}
