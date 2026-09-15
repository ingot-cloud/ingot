package com.ingot.cloud.iam.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.commons.model.types.UserType;
import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.oss.common.OssUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 系统用户
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Data
@Schema(description = "系统用户")
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseModel<SysUser> implements UserType {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码
     */
    @JsonIgnore
    @Schema(description = "密码")
    private String password;

    /**
     * 是否必须修改密码
     */
    @JsonIgnore
    @Schema(description = "是否必须修改密码")
    private Boolean mustChangePwd;

    /**
     * 密码最后修改时间
     */
    @JsonIgnore
    @Schema(description = "密码最后修改时间")
    private LocalDateTime passwordChangedAt;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 手机号
     */
    @Sensitive(mode = SensitiveMode.MOBILE_PHONE)
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮件地址
     */
    @Sensitive(mode = SensitiveMode.EMAIL)
    @Schema(description = "邮件地址")
    private String email;

    /**
     * 头像
     */
    @Schema(description = "头像")
    @OssUrl
    private String avatar;

    /**
     * 是否启用（1-启用 0-禁用）
     */
    @Schema(description = "是否启用")
    private Boolean enabled;

    /**
     * 是否锁定（1-锁定 0-正常，冗余字段，详情见 account_lock_state 表）
     */
    @Schema(description = "是否锁定")
    private Boolean locked;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    /**
     * 乐观锁版本号
     */
    @Version
    @Schema(description = "版本号")
    private Long version;

    /**
     * 创建日期
     */
    @Schema(description = "创建日期")
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    @Schema(description = "更新日期")
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    @Schema(description = "删除日期")
    private LocalDateTime deletedAt;

}
