package com.ingot.framework.security.credential.policy;

import java.util.Set;

import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.PolicyCheckContext;

/**
 * 密码策略接口
 * <p>所有密码策略都必须实现此接口</p>
 *
 * @author jymot
 * @since 2026-01-21
 */
public interface PasswordPolicy {

    /**
     * 获取策略类型
     */
    CredentialPolicyType getType();

    /**
     * 获取策略优先级（数字越小优先级越高）
     */
    int getPriority();

    /**
     * 是否为阻断式策略
     * <p>如果为 true，当该策略校验失败时，后续策略不再执行</p>
     */
    default boolean isBlocking() {
        return false;
    }

    /**
     * 获取该策略适用的场景
     * <p>如果返回空集合，表示适用于所有场景</p>
     */
    default Set<CredentialScene> getApplicableScenes() {
        return Set.of(); // 默认适用于所有场景
    }

    /**
     * 判断该策略是否适用于指定场景
     */
    default boolean isApplicableToScene(CredentialScene scene) {
        Set<CredentialScene> scenes = getApplicableScenes();
        // 空集合表示适用于所有场景
        return scenes.isEmpty() || scenes.contains(scene);
    }

    /**
     * 执行策略检查
     *
     * @param context 检查上下文
     * @return 检查结果
     */
    PasswordCheckResult check(PolicyCheckContext context);
}
