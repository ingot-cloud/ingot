package com.ingot.cloud.iam.organization;

import java.util.Set;

import com.ingot.framework.commons.model.iam.AuthorizationContext;

/**
 * <p>在成员写事务内校验实际 ACTION 与旧、新归属，禁止把预览结果当作提交凭证。</p>
 *
 * <p>实现必须使用同一事务可见的最新授权。状态及移出要求覆盖全部部门；调部门仅校验受影响关系。
 * 这是服务器内部接口，不能从请求参数、布尔 allowed 或客户端范围构造实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FunctionalInterface
public interface MemberMutationGuard {
    /**
     * 校验本次写入的操作权和对象范围；拒绝时必须抛出异常，不允许静默过滤。
     * @param actor 事务中重新确认的当前身份
     * @param operation 本次成员操作
     * @param memberId 当前域内成员 ID
     * @param oldDepartments 状态/移出为全部部门；调整关系为被删除或主部门标记变化的旧关系
     * @param newDepartments 调整关系为新增或主部门标记变化的新关系；其他操作为空
     */
    void require(AuthorizationContext actor, Operation operation, String memberId,
                 Set<String> oldDepartments, Set<String> newDepartments);

    /**
     * <p>区分成员资格变更与任职调整的 ACTION 和范围覆盖要求。</p>
     * @author jy
     * @since 1.0.0
     */
    enum Operation {
        CHANGE_STATUS, REMOVE, CHANGE_DEPARTMENTS
    }
}
