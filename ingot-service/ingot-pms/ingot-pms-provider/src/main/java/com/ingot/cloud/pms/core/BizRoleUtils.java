package com.ingot.cloud.pms.core;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.MetaRoleService;
import com.ingot.cloud.pms.service.domain.TenantRolePrivateService;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;

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

    /**
     * 获取用户角色
     *
     * @param userId                       用户ID
     * @param metaRoleService              Meta角色服务
     * @param tenantRoleUserPrivateService 角色用户服务
     * @param tenantRolePrivateService     角色组织服务
     * @return 角色列表
     */
    public static List<RoleType> getUserRoles(long userId, MetaRoleService metaRoleService,
                                       TenantRoleUserPrivateService tenantRoleUserPrivateService,
                                       TenantRolePrivateService tenantRolePrivateService) {
        List<TenantRoleUserPrivate> roleUserPrivateList = tenantRoleUserPrivateService.getUserRoles(userId);
        if (CollUtil.isEmpty(roleUserPrivateList)) {
            return ListUtil.empty();
        }

        List<RoleType> result = new ArrayList<>(roleUserPrivateList.size());

        List<Long> metaRoleIds = roleUserPrivateList.stream()
                .filter(item -> BooleanUtil.isTrue(item.getMetaRole()))
                .map(TenantRoleUserPrivate::getRoleId)
                .toList();
        if (CollUtil.isNotEmpty(metaRoleIds)) {
            List<MetaRole> metaRoleList = metaRoleService.list(Wrappers.<MetaRole>lambdaQuery()
                    .eq(MetaRole::getStatus, CommonStatusEnum.ENABLE)
                    .in(MetaRole::getId, metaRoleIds));
            result.addAll(metaRoleList);
        }

        List<Long> privateRoleIds = roleUserPrivateList.stream()
                .filter(item -> BooleanUtil.isFalse(item.getMetaRole()))
                .map(TenantRoleUserPrivate::getRoleId)
                .toList();
        if (CollUtil.isNotEmpty(privateRoleIds)) {
            List<TenantRolePrivate> privateRoleList = tenantRolePrivateService.list(Wrappers.<TenantRolePrivate>lambdaQuery()
                    .eq(TenantRolePrivate::getStatus, CommonStatusEnum.ENABLE)
                    .in(TenantRolePrivate::getId, privateRoleIds));
            result.addAll(privateRoleList);
        }

        return result;
    }
}
