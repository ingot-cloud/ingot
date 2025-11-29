package com.ingot.cloud.member.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Getter
@Setter
@ToString
@TableName("member_user_tenant")
public class MemberUserTenant extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 是否为主要租户
     */
    private Boolean main;

    /**
     * 租户名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
