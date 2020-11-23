package com.ingot.framework.store.tenant;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.core.context.ContextHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * <p>Description  : TenantRequestInterceptor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:38 下午.</p>
 */
public class TenantRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request,
                                        @NonNull byte[] body,
                                        @NonNull ClientHttpRequestExecution execution)
            throws IOException {

        if (StrUtil.isNotEmpty(ContextHolder.tenantID())) {
            request.getHeaders().set(TenantConstants.TENANT_HEADER_KEY, ContextHolder.tenantID());
        }

        return execution.execute(request, body);
    }

}
