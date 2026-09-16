package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>角色版本与定义联查投影，仅用于分配校验，不对应独立表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
public class IamRoleRevisionJoin {
    /**
     * 角色版本 ID。
     */
    private BigInteger id;

    /**
     * 角色版本种类。
     */
    private RoleKind kind;

    /**
     * 定义是否启用。
     */
    private Boolean enabled;

    /**
     * 定义所属授权域。
     */
    private AuthorizationDomain domain;

    /**
     * 定义所属租户，平台与共享角色为空。
     */
    private BigInteger tenantId;
}
