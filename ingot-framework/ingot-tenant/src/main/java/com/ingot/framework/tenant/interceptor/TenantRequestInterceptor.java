package com.ingot.framework.tenant.interceptor;

import java.io.IOException;

import com.ingot.framework.core.constants.HeaderConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

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

        if (TenantContextHolder.get() != null) {
            request.getHeaders().set(HeaderConstants.TENANT,
                    String.valueOf(TenantContextHolder.get()));
        }

        return execution.execute(request, body);
    }

}
