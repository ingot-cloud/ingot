package com.ingot.cloud.auth.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.auth.model.dto.UserTokenQueryDTO;
import com.ingot.cloud.auth.model.vo.OAuth2RegisteredClientVO;
import com.ingot.cloud.auth.model.vo.UserTokenVO;
import com.ingot.cloud.auth.service.biz.BizUserTokenService;
import com.ingot.cloud.auth.service.domain.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.rpc.RemotePmsTenantDetailsService;
import com.ingot.cloud.pms.api.rpc.RemotePmsUserDetailsService;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizUserTokenServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/21.</p>
 * <p>Time         : 13:33.</p>
 */
@Service
@RequiredArgsConstructor
public class BizUserTokenServiceImpl implements BizUserTokenService {
    private final OnlineTokenService onlineTokenService;
    private final RemotePmsUserDetailsService remotePmsUserService;
    private final RemotePmsTenantDetailsService remotePmsTenantDetailsService;

    private final Oauth2RegisteredClientService clientService;

    @Override
    public IPage<UserTokenVO> userTokenPage(UserTokenQueryDTO params) {
        long limit = (params.getCurrent() - 1) * params.getSize();
        long tenantId = params.getTenantId();
        String clientId = params.getClientId();

        long total = onlineTokenService.getOnlineUserCount(tenantId, clientId);
        if (total == 0) {
            return Page.of(params.getCurrent(), params.getSize());
        }
        Page<UserTokenVO> page = Page.of(params.getCurrent(), params.getSize(), total);

        List<Long> userIds = onlineTokenService.getOnlineUsers(
                tenantId, clientId, limit, params.getSize());
        if (CollUtil.isEmpty(userIds)) {
            return page;
        }

        SysTenant tenant = remotePmsTenantDetailsService.getTenantById(tenantId)
                .ifError(OAuth2ErrorUtils::checkResponse)
                .getData();
        OAuth2RegisteredClientVO client = clientService.getByClientId(clientId);

        page.setRecords(remotePmsUserService.getAllUserInfo(userIds)
                .ifError(OAuth2ErrorUtils::checkResponse)
                .getData()
                .stream()
                .map(user -> {
                    UserTokenVO vo = new UserTokenVO();
                    vo.setUserId(user.getId());
                    vo.setAvatar(user.getAvatar());
                    vo.setNickname(user.getNickname());
                    vo.setTenantLogo(tenant.getAvatar());
                    vo.setTenantName(tenant.getName());
                    vo.setClientName(client.getClientName());
                    vo.setTokens(onlineTokenService.getUserAllTokens(user.getId(), tenantId, clientId));
                    return vo;
                }).toList());
        return page;
    }
}
