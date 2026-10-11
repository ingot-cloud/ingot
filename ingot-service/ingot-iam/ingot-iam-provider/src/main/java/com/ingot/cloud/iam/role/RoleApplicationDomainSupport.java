package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.entity.IamRoleApplicationTaskEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.support.IamAdmission;
import com.ingot.framework.commons.model.iam.AssignmentRecord;
import com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput;
import com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeResult;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.RolePublishInput;
import com.ingot.framework.commons.model.iam.RoleRevisionDistribution;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;

/**
 * <p>封装角色版本应用的业务域访问与门禁，任务状态和事务由共用流程管理。</p>
 * <p>仅供IAM内部使用；实现必须恢复当前身份和授权，不能缓存准入或开启独立事务。</p>
 * @author jy
 * @since 1.0.0
 */
interface RoleApplicationDomainSupport {
    /** 检查当前请求的业务域身份和独立分配治理资格。 */
    IamAdmission admit();

    /** 从持久任务身份标识恢复当前有效身份并重新校验治理资格，不使用HTTP上下文。 */
    IamAdmission restore(IamRoleApplicationTaskEntity task);

    /**
     * 返回治理对象范围。
     * @param fresh 写入与执行时必须为true，普通展示读取为false
     */
    ObjectScope scope(IamAdmission admission, boolean fresh);

    /** 检查角色归属、支持的角色种类和READ对象范围；fresh用于事务内执行。 */
    void requireRole(String roleId, IamAdmission admission, boolean fresh);

    /** 在调用方事务内按授权→角色顺序锁定；身份及任务锁仍由调用方按原顺序处理。 */
    void lockRole(long roleId);

    /** 在调用方事务内沿用既有发布门禁，返回新版本标识。 */
    CreatedResource publish(String roleId, RolePublishInput input);

    /** 读取目标固定版本并验证业务域、支持的角色种类及同角色归属。 */
    IamRoleRevisionEntity requireTarget(String roleId, String revisionId);

    /** 读取固定版本；调用方必须已验证角色可见性，并自行检查来源版本与角色关联。 */
    IamRoleRevisionEntity findRevision(long revisionId);

    /** 构造当前业务域的目标版本引用，不额外查询数据库。 */
    RoleRevisionRef reference(BigInteger revisionId);

    /** 按同角色关联与对象范围游标查询；来源版本和分配ID为null时不附加对应过滤。 */
    List<IamRoleAssignmentEntity> scanAssignments(long roleId, BigInteger after,
            List<BigInteger> sourceIds, List<BigInteger> assignmentIds, ObjectScope scope, int limit);

    /** 在数据库内分页读取同一可见性边界的角色关联分配。 */
    Page<IamRoleAssignmentEntity> pageAssignments(long roleId, int page, int size, ObjectScope scope);

    /** 使用与候选相同的关联和范围谓词汇总版本分布。 */
    List<RoleRevisionDistribution> distribution(long roleId, ObjectScope scope);

    /** 按当前业务域读取分配，调用方在原位置检查对象范围和乐观锁。 */
    IamRoleAssignmentEntity findAssignment(long assignmentId);

    /** 沿用治理门禁取得分配主体、范围、期限和来源的完整快照。 */
    AssignmentRecord snapshot(IamAdmission admission, IamRoleAssignmentEntity assignment);

    /** 复用范围兼容及委派校验预检一个批次；不得写入分配。 */
    AssignmentUpgradeResult preview(IamAdmission admission, AssignmentUpgradeInput input);

    /** 在调用方事务内重新校验并写入批次，包含分配审计和提交后缓存失效。 */
    void apply(IamAdmission admission, AssignmentUpgradeInput input);
}
