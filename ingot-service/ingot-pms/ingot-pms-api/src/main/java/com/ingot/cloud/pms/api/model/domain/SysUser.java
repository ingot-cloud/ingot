package com.ingot.cloud.pms.api.model.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.cloud.pms.api.model.types.UserType;
import com.ingot.framework.core.model.enums.UserStatusEnum;
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
 *
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
     * 初始化密码标识
     */
    @Schema(description = "初始化密码标识")
    private Boolean initPwd;

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
     * 状态, 0:正常，9:禁用
     */
    @Schema(description = "状态")
    private UserStatusEnum status;

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
