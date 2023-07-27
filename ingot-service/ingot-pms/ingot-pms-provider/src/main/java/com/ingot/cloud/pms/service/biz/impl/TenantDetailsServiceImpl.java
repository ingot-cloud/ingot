package com.ingot.cloud.pms.service.biz.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.biz.TenantDetailsService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.core.model.dto.common.AllowTenantDTO;
import com.ingot.framework.security.core.tenantdetails.TenantDetailsResponse;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private final SysUserService sysUserService;

    @Override
    public TenantDetailsResponse getUserTenantDetails(String username) {
        TenantDetailsResponse response = new TenantDetailsResponse();

        List<SysUser> userList = TenantEnv.globalApply(() -> sysUserService.list(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)));
        List<AllowTenantDTO> allows = sysTenantService.list(Wrappers.<SysTenant>lambdaQuery().in(SysTenant::getId,
                        userList.stream()
                                .map(SysUser::getTenantId)
                                .collect(Collectors.toList())))
                .stream()
                .map(item -> {
                    AllowTenantDTO dto = new AllowTenantDTO();
                    dto.setId(item.getId());
                    dto.setName(dto.getName());
                    return dto;
                }).collect(Collectors.toList());

        response.setAllows(allows);
        return response;
    }
}
