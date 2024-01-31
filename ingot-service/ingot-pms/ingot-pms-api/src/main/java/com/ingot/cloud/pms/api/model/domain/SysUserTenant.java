package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.pms.api.model.types.UserTenantType;
import com.ingot.framework.data.mybatis.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-09-02
 */
@Getter
@Setter
@TableName("sys_user_tenant")
public class SysUserTenant extends BaseModel<SysUserTenant> implements UserTenantType {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
