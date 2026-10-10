package com.ingot.framework.authorization.field;

import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>隔离本地和远程授权；写求值必须读取当前分配、资源和策略，不复用预览结论。</p>
 * @author jy
 * @since 1.0.0
 */
public interface FieldPolicyProvider {
    /** 获取短期只读快照。 */
    AuthorizationDecision read(ResourceKey resource, String actionCode);
    /** 在业务事务内 fresh 求值精确写操作，非写操作必须拒绝。 */
    AuthorizationDecision write(ResourceKey resource, String actionCode);
}
