package com.ingot.cloud.member.api.model.domain;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
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
@TenantTable
@TableName("member_role_user")
public class MemberRoleUser extends BaseModel<MemberRoleUser> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;
}
