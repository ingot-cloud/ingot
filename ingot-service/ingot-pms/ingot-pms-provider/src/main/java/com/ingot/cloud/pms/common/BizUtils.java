package com.ingot.cloud.pms.common;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.UserStatusEnum;

/**
 * <p>Description  : BizUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/14.</p>
 * <p>Time         : 10:38 AM.</p>
 */
public class BizUtils {

    /**
     * 获取 {@link TenantMainDTO} 列表
     */
    public static List<TenantMainDTO> getAllows(SysTenantService sysTenantService,
                                                Set<Long> userTenantList,
                                                Consumer<TenantMainDTO> mainConsumer) {
        return sysTenantService.list(
                        Wrappers.<SysTenant>lambdaQuery()
                                .in(SysTenant::getId, userTenantList))
                .stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .map(item -> {
                    TenantMainDTO dto = new TenantMainDTO();
                    dto.setId(String.valueOf(item.getId()));
                    dto.setName(item.getName());
                    dto.setAvatar(item.getAvatar());
                    mainConsumer.accept(dto);
                    return dto;
                })
                .toList();
    }

    /**
     * 根据当前用户状态和可访问租户列表，返回用户最终状态
     */
    public static UserStatusEnum getUserStatus(List<TenantMainDTO> allows, UserStatusEnum userStatus, Long loginTenant) {
        // 没有允许访问的租户，那么直接返回不可用
        if (CollUtil.isEmpty(allows)) {
            return UserStatusEnum.LOCK;
        }
        // 如果允许访问的tenant中不存在当前登录的tenant，那么直接返回不可用
        if (loginTenant != null && allows.stream().noneMatch(item -> Long.parseLong(item.getId()) == loginTenant)) {
            return UserStatusEnum.LOCK;
        }
        return userStatus;
    }

    /**
     * 转换RoleType为RoleTreeNodeVO
     *
     * @param role        {@link RoleType}
     * @param roleConvert {@link RoleConvert}
     * @return {@link RoleTreeNodeVO}
     */
    public static RoleTreeNodeVO convert(RoleType role, RoleConvert roleConvert) {
        RoleTreeNodeVO item = roleConvert.to(role);
        item.setTypeText(role.getType().getText());
        item.setOrgTypeText(role.getOrgType().getText());
        item.setScopeTypeText(role.getScopeType().getText());
        item.setStatusText(role.getStatus().getText());
        return item;
    }
}
