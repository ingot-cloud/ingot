package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射iam_role_application_task；不保存会话凭证。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_application_task", autoResultMap = true)
public class IamRoleApplicationTaskEntity {
    /** 任务标识。 */
    @TableId(type = IdType.INPUT)
    private BigInteger id;
    /** 角色标识。 */
    private BigInteger roleId;
    /** 目标版本。 */
    private BigInteger targetRevisionId;
    /** 执行者可信账号。 */
    private BigInteger actorAccountId;
    /** 执行者可信平台成员。 */
    private BigInteger actorMemberId;
    /** 发起成员。 */
    private BigInteger createdBy;
    /** 集合方式。 */
    private RoleApplicationSelection selection;
    /** 来源及选中ID快照JSON。 */
    private String selectionSnapshot;
    /** 任务状态。 */
    private RoleApplicationStatus status;
    /** 计划乐观锁。 */
    private BigInteger version;
    /** 准备幂等键。 */
    private String prepareKey;
    /** 确认幂等键。 */
    private String confirmKey;
    /** 执行节点令牌。 */
    private String leaseOwner;
    /** UTC租约期限。 */
    private LocalDateTime leaseUntil;
    /** 暂停原因。 */
    private String failureReason;
    /** UTC创建时间。 */
    private LocalDateTime createdAt;
    /** UTC更新时间。 */
    private LocalDateTime updatedAt;
}
