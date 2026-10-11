package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射iam_role_application_item；不保存会话凭证。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_application_item", autoResultMap = true)
public class IamRoleApplicationItemEntity {
    /** 明细标识。 */
    @TableId(type = IdType.INPUT)
    private BigInteger id;
    /** 所属任务。 */
    private BigInteger taskId;
    /** 分配标识。 */
    private BigInteger assignmentId;
    /** 原分配JSON快照。 */
    private String originalSnapshot;
    /** 预期分配乐观锁。 */
    private BigInteger expectedAssignmentVersion;
    /** 参数JSON。 */
    private String configuredBindings;
    /** 差异JSON。 */
    private String previewSnapshot;
    /** 处理状态。 */
    private RoleApplicationItemStatus status;
    /** 冲突原因。 */
    private String failureReason;
}
