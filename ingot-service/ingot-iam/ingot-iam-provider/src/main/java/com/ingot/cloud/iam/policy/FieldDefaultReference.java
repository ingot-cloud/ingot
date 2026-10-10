package com.ingot.cloud.iam.policy;

import java.time.Instant;
import java.util.Objects;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime;

/**
 * <p>不含可变实体引用的默认策略来源快照，保留 loader 取得事实时的绝对期限。</p>
 * @param definition 固定版本的 JSON 定义
 * @param expiresAt 来源期限
 * @author jy
 * @since 1.0.0
 */
public record FieldDefaultReference(String definition, Instant expiresAt) {
    /** 固定缓存契约，缺失期限不能作为有效来源。 */
    public FieldDefaultReference {
        Objects.requireNonNull(definition);
        Objects.requireNonNull(expiresAt);
    }

    /** 从刚刚读取的持久化事实创建不可变来源；合法不存在不缓存。 */
    public static FieldDefaultReference from(IamDefaultPolicyRevisionEntity value) {
        return value == null ? null : new FieldDefaultReference(value.getDefinition(), FieldPolicyLifetime.deadline());
    }
}
