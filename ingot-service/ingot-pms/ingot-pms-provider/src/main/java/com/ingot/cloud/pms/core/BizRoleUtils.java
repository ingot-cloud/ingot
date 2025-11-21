package com.ingot.cloud.pms.core;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;

/**
 * <p>Description  : BizRoleUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/20.</p>
 * <p>Time         : 17:51.</p>
 */
public class BizRoleUtils {

    /**
     * 确保用户包含指定角色，如果当前已绑定指定角色
     *
     * @param userId                 用户ID
     * @param roles                  角色ID列表
     * @param roleCode               角色编码
     * @param roleService            角色服务
     * @param roleUserPrivateService 角色用户服务
     */
    public static void ensureRoles(long userId,
                                   List<Long> roles,
                                   String roleCode,
                                   BizRoleService roleService,
                                   TenantRoleUserPrivateService roleUserPrivateService) {
        // 如果当前组织包含指定角色，那么需要判断该用户是否有当前指定角色，如果有则确保该角色不被删除
        RoleType ensureRole = roleService.getByCode(roleCode);
        if (ensureRole != null) {
            long count = roleUserPrivateService.count(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                    .eq(TenantRoleUserPrivate::getUserId, userId)
                    .eq(TenantRoleUserPrivate::getRoleId, ensureRole.getId()));
            if (count > 0) {
                roles.add(ensureRole.getId());
            }
        }
    }
}
