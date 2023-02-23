package com.ingot.cloud.pms.api.model.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.store.mybatis.model.BaseModel;
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
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseModel<SysUser> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 所属租户
     */
    @JsonIgnoreProperties(allowSetters = true)
    private Long tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 用户名
     */
    @NotBlank(message = "{SysUser.username}", groups = Group.Create.class)
    private String username;

    /**
     * 密码
     */
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
