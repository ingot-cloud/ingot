package com.ingot.framework.security.core.tenantdetails;

import java.util.List;
import java.util.Optional;

import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.security.TenantDetailsRequest;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : RemoteTenantDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 4:52 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultTenantDetailsService implements TenantDetailsService {
    private final RemoteTenantDetailsService remoteTenantDetailsService;

    @Override
    public TenantDetails loadByUsername(String username) {
        TenantDetailsRequest request = new TenantDetailsRequest();
        request.setUsername(username);
        return parse(remoteTenantDetailsService.getAllowList(request));
    }

    private TenantDetails parse(R<TenantDetailsResponse> response) {
        return Optional.of(response)
                .map(r -> {
                    OAuth2ErrorUtils.checkResponse(response);
                    return r.getData();
                })
                .map(data -> {
                    List<AllowTenantDTO> allowList = Optional.ofNullable(data.getAllows()).orElse(ListUtil.empty());
                    return new Tenant(allowList);
                })
                .orElse(Tenant.EMPTY);
    }
}
