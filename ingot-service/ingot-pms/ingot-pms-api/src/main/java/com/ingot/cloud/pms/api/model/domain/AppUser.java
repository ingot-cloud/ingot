package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Getter
@Setter
@TableName("app_user")
public class AppUser extends BaseModel<AppUser> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "{SysUser.username}", groups = Group.Create.class)
    private String username;

    /**
     * 密码
     */
    @TableField("`password`")
    @JsonIgnore
    private String password;

    /**
     * 初始化密码标识
     */
    private Boolean initPwd;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    @Sensitive(mode = SensitiveMode.MOBILE_PHONE)
    private String phone;

    /**
     * 邮件地址
     */
    @Sensitive(mode = SensitiveMode.EMAIL)
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态, 0:正常，9:禁用
     */
    @TableField("`status`")
    private UserStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
