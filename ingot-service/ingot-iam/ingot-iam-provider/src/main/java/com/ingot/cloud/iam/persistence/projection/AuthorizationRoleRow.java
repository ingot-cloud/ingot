package com.ingot.cloud.iam.persistence.projection;

import java.math.BigInteger;
import com.ingot.framework.commons.model.iam.RoleKind;

/**
 * <p>角色树层级的轻量数据库投影，角色与版本使用真实关联标识。</p>
 *
 * @param id 当前层节点标识
 * @param roleId 所属角色标识
 * @param name 角色名称
 * @param kind 版本种类；角色层为空
 * @param revision 版本号；角色层为空
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationRoleRow(BigInteger id, BigInteger roleId, String name,
        RoleKind kind, BigInteger revision) {
}
