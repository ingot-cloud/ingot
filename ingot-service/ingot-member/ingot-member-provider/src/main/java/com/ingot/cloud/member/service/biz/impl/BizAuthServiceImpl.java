package com.ingot.cloud.member.service.biz.impl;

import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.member.api.model.convert.MemberUserConvert;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.cloud.member.api.model.dto.user.MemberUserInfoDTO;
import com.ingot.cloud.member.service.biz.BizAuthService;
import com.ingot.cloud.member.service.domain.MemberUserService;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.cloud.pms.api.rpc.PmsTenantDetailsService;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizAuthServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 17:30.</p>
 */
@Service
@RequiredArgsConstructor
public class BizAuthServiceImpl implements BizAuthService {
    private final MemberUserTenantService userTenantService;
    private final MemberUserService userService;
    private final PmsTenantDetailsService pmsTenantDetailsService;

    @Override
    public MemberUserInfoDTO getUserInfo(InUser user) {
        // 使用当前用户 tenant 进行操作
        return TenantEnv.applyAs(user.getTenantId(), () -> {
            Long userId = user.getId();

            MemberUser userInfo = userService.getById(userId);
            if (userInfo == null) {
                OAuth2ErrorUtils.throwInvalidRequest("用户异常");
            }

            // 获取可以访问的租户列表
            List<MemberUserTenant> userTenantList = userTenantService.getUserOrgs(userId);
            List<AllowTenantDTO> allows = pmsTenantDetailsService.getTenantByIds(userTenantList.stream()
                            .map(MemberUserTenant::getTenantId)
                            .distinct()
                            .collect(Collectors.toList()))
                    .ifError(OAuth2ErrorUtils::checkResponse)
                    .getData().getAllows();
            if (CollUtil.isNotEmpty(allows)) {
                allows.forEach(item -> {
                    // main=true，为当前登录的租户
                    item.setMain(Long.parseLong(item.getId()) == user.getTenantId());
                });
            }

            MemberUserInfoDTO result = new MemberUserInfoDTO();
            result.setUser(MemberUserConvert.INSTANCE.toUserBaseInfo(userInfo));
            result.setRoles(user.getRoleCodeList());
            result.setAllows(allows);
            return result;
        });
    }
}
