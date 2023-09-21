package com.ingot.cloud.pms.core;

import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnums;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Description  : TenantEngine.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/1.</p>
 * <p>Time         : 4:25 PM.</p>
 */
@Component
@RequiredArgsConstructor
public class TenantEngine {
    private final SysRoleService sysRoleService;

    //  todo，
    //   初始化管理后台角色和app角色(默认租户的组织类型角色)，初始化部门，初始化管理员用户

    private static final List<String> DEFAULT_ROLES = ListUtil.list(false,
            RoleConstants.ROLE_MANAGER_CODE, RoleConstants.ROLE_ORG_SUB_ADMIN_CODE);

    /**
     * 初始化租户默认数据
     *
     * @param tenantId 租户ID
     */
    public void initDefault(Long tenantId) {
        TenantEnv.runAs(tenantId, () -> {
            LocalDateTime now = DateUtils.now();
            List<SysRole> roles = DEFAULT_ROLES.stream()
                    .map(item -> {
                        SysRole role = new SysRole();
                        role.setCode(item);
                        role.setName(item);
                        role.setType(RoleTypeEnums.System);
                        role.setStatus(CommonStatusEnum.ENABLE);
                        role.setCreatedAt(now);
                        role.setUpdatedAt(now);
                        return role;
                    }).collect(Collectors.toList());
            sysRoleService.saveBatch(roles);
        });
    }

    /**
     * 删除租户相关数据
     *
     * @param tenantId 租户ID
     */
    public void remove(Long tenantId) {
        TenantEnv.runAs(tenantId, () -> {
            List<SysRole> list = sysRoleService.list();
            sysRoleService.removeBatchByIds(list);
        });
    }
}
