package com.ingot.cloud.pms.service.biz.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.TenantDetailsService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.security.TenantDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>Description  : TenantDetailsServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 4:39 PM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDetailsServiceImpl implements TenantDetailsService {
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;
    private final SysUserService sysUserService;

    @Override
    public TenantDetailsResponse getUserTenantDetails(String username) {
        TenantDetailsResponse response = new TenantDetailsResponse();

        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        List<SysUserTenant> userTenantList = sysUserTenantService.list(
                Wrappers.<SysUserTenant>lambdaQuery()
                        .eq(SysUserTenant::getUserId, user.getId()));

        List<AllowTenantDTO> allows = BizUtils.getAllows(sysTenantService,
                userTenantList.stream()
                        .map(SysUserTenant::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t -> Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())));
        response.setAllows(allows);
        return response;
    }
}
