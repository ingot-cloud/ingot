package com.ingot.cloud.pms.common;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.enums.UserStatusEnum;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>Description  : BizUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/14.</p>
 * <p>Time         : 10:38 AM.</p>
 */
public class BizUtils {

    /**
     * 获取 {@link AllowTenantDTO} 列表
     */
    public static List<AllowTenantDTO> getAllows(SysTenantService sysTenantService,
                                                 Set<Long> userTenantList,
                                                 Consumer<AllowTenantDTO> mainConsumer) {
        return sysTenantService.list(
                        Wrappers.<SysTenant>lambdaQuery()
                                .in(SysTenant::getId, userTenantList))
                .stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .map(item -> {
                    AllowTenantDTO dto = new AllowTenantDTO();
                    dto.setId(item.getId());
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
    public static UserStatusEnum getUserStatus(List<AllowTenantDTO> allows, UserStatusEnum userStatus) {
        UserStatusEnum userTenantStatus = CollUtil.isEmpty(allows)
                ? UserStatusEnum.LOCK : UserStatusEnum.ENABLE;
        return userStatus == UserStatusEnum.ENABLE
                && userTenantStatus == UserStatusEnum.ENABLE ?
                UserStatusEnum.ENABLE : UserStatusEnum.LOCK;
    }
}
