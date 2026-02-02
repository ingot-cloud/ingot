package com.ingot.framework.security.credential.policy;

import java.util.List;
import java.util.Set;

import com.ingot.framework.security.credential.model.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;

/**
 * 密码历史策略
 * <p>防止用户重复使用最近的密码</p>
 * <p>适用场景：修改密码</p>
 *
 * @author jymot
 * @since 2026-01-21
 */
@Setter
@Getter
public class PasswordHistoryPolicy implements PasswordPolicy {
    private static final int DEFAULT_PRIORITY = 30;

    private final PasswordEncoder passwordEncoder;
    private int checkCount = 5;
    private boolean enabled = true;

    public PasswordHistoryPolicy(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CredentialPolicyType getType() {
        return CredentialPolicyType.HISTORY;
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public boolean isBlocking() {
        return true; // 阻断式策略
    }

    @Override
    public Set<CredentialScene> getApplicableScenes() {
        // 只适用于修改密码场景
        return Set.of(CredentialScene.CHANGE_PASSWORD);
    }

    @Override
    public PasswordCheckResult check(PolicyCheckContext context) {
        if (!enabled) {
            return PasswordCheckResult.pass();
        }

        List<String> oldPasswordHashes = context.getOldPasswordHashes();

        // 如果没有历史密码，直接通过
        if (CollectionUtils.isEmpty(oldPasswordHashes)) {
            return PasswordCheckResult.pass();
        }

        String newPassword = context.getPassword();

        // 检查新密码是否与历史密码重复
        for (String oldHash : oldPasswordHashes) {
            if (passwordEncoder.matches(newPassword, oldHash)) {
                return PasswordCheckResult.fail(
                        String.format("该密码已在最近%d次使用过，请更换新密码", checkCount),
                        CredentialErrorCode.HISTORY_REUSE
                ).addMetadata("checkCount", checkCount);
            }
        }

        return PasswordCheckResult.pass();
    }
}
