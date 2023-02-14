package com.ingot.framework.tenant.interceptor;

import com.ingot.framework.core.constants.HeaderConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : TenantFeignInterceptor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:32 下午.</p>
 */
@Slf4j
public class TenantFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (TenantContextHolder.get() == null) {
            log.debug("Feign 请求拦截器 tenantID 为空");
            return;
        }
        requestTemplate.header(HeaderConstants.TENANT,
                String.valueOf(TenantContextHolder.get()));
    }
}
