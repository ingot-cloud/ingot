package com.ingot.cloud.member.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.commons.model.types.UserType;
import com.ingot.framework.core.utils.sensitive.Sensitive;
import com.ingot.framework.core.utils.sensitive.SensitiveMode;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import com.ingot.framework.oss.common.OssUrl;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 会员用户
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Getter
@Setter
@ToString
@TableName("member_user")
public class MemberUser extends BaseModel<MemberUser> implements UserType {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    @JsonIgnore
    @TableField("`password`")
    private String password;

    /**
     * 是否必须修改密码
     */
    @JsonIgnore
    private Boolean mustChangePwd;

    /**
     * 密码最后修改时间
     */
    @JsonIgnore
    private LocalDateTime passwordChangedAt;

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
    @OssUrl
    private String avatar;

    /**
     * 是否启用（true-启用 false-禁用）
     */
    private Boolean enabled;

    /**
     * 是否锁定（true-锁定 false-正常，冗余字段，详情见 account_lock_state 表）
     */
    private Boolean locked;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

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
