package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存全局账号持久化字段，不承载成员资料或权限。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_account", autoResultMap = true)
public class IamAccountEntity {
    /** 全局账号 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 登录名。 */
    @TableField("username")
    private String username;

    /** 凭证哈希，仅内部认证读取。 */
    @TableField("password_hash")
    private String passwordHash;

    /** 全局登录手机号，可空。 */
    @TableField("phone")
    private String phone;

    /** 全局邮箱，可空。 */
    @TableField("email")
    private String email;

    /** 全局启用状态。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 是否必须修改密码。 */
    @TableField("must_change_password")
    private Boolean mustChangePassword;

    /** 最近改密时间，UTC。 */
    @TableField("password_changed_at")
    private LocalDateTime passwordChangedAt;

    /** 最近登录时间，UTC。 */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /** 递增配置版本，不使用旧时间戳乐观锁插件。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间，UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 软删除时间，UTC；为空表示未删除。 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

}
