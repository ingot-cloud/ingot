package com.ingot.framework.store.tenant;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.context.ContextHolder;
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
        if (StrUtil.isEmpty(ContextHolder.tenantID())) {
            log.debug("Feign 请求拦截器 tenantID 为空");
            return;
        }
        requestTemplate.header(TenantConstants.TENANT_HEADER_KEY, ContextHolder.tenantID());
    }
}
