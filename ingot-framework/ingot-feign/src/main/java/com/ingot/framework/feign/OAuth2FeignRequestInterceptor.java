package com.ingot.framework.feign;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.core.context.RequestContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;


/**
 * <p>Description  : OAuth2FeignRequestInterceptor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/13.</p>
 * <p>Time         : 上午8:40.</p>
 */
@Slf4j
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {
    private final static List<String> relayHeaders = Lists.newArrayList(HttpHeaders.AUTHORIZATION, "deviceid");

    public OAuth2FeignRequestInterceptor() {
    }

    @Override public void apply(RequestTemplate template) {
        log.info(">>> OAuth2FeignRequestInterceptor - start.");

        relayHeader(template);

        // 设置 feign 为内部请求
        template.header(SecurityConstants.HEADER_FROM, SecurityConstants.HEADER_FROM_INSIDE_VALUE);

        log.info(">>> OAuth2FeignRequestInterceptor - end.");
    }

    /**
     * 转发 header 到 RequestTemplate
     */
    private void relayHeader(RequestTemplate template){
        HttpServletRequest request = RequestContextHolder.getRequest().orElse(null);
        if (request == null){
            return;
        }

        // relay header
        Enumeration<String> headerNames = request.getHeaderNames();
        String headerName;
        while (headerNames.hasMoreElements()){
            headerName = headerNames.nextElement();
            if (!relayHeaders.contains(headerName.toLowerCase())){
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            List<String> headerValues = Collections.list(values);
            log.info(">>> OAuth2FeignRequestInterceptor - relay header >> set key={}, values={}", headerName, headerValues);
            template.header(headerName, headerValues);
        }

    }
}
