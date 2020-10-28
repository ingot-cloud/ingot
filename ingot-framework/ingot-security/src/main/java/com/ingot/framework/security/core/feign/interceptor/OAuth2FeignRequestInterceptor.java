package com.ingot.framework.security.core.feign.interceptor;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.core.utils.RequestUtils;
import com.ingot.framework.security.utils.ClientTokenUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.ingot.framework.core.constants.SecurityConstants.OAUTH2_BEARER_TYPE;


/**
 * <p>Description  : OAuth2FeignRequestInterceptor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/13.</p>
 * <p>Time         : 上午8:40.</p>
 */
@Slf4j
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {
    private static List<String> relayHeaders = Lists.newArrayList("authorization", "deviceid");

    private final ClientTokenUtils clientTokenUtils;

    public OAuth2FeignRequestInterceptor(ClientTokenUtils clientTokenUtils) {
        Assert.notNull(clientTokenUtils, "Context can not be null");
        this.clientTokenUtils = clientTokenUtils;
    }

    @Override public void apply(RequestTemplate template) {
        log.info(">>> OAuth2FeignRequestInterceptor - start.");

        relayHeader(template);

        relayBearerToken(template);

        // feign 为内部请求
        template.header(SecurityConstants.HEADER_FROM, SecurityConstants.HEADER_FROM_INSIDE_VALUE);

        log.info(">>> OAuth2FeignRequestInterceptor - end.");
    }

    private void relayBearerToken(RequestTemplate template){
        Map<String, Collection<String>> headers = template.headers();

        List<String> authorizeList = Lists.newArrayList();
        // authorizeHeaders 为不可变容器
        Collection<String> authorizeHeaders = headers.get(HttpHeaders.AUTHORIZATION);
        if (CollUtil.isEmpty(authorizeHeaders)){
            authorizeHeaders = headers.get(HttpHeaders.AUTHORIZATION.toLowerCase());
            if (CollUtil.isEmpty(authorizeHeaders)){
                authorizeHeaders = new ArrayList<>();
            }
        }

        authorizeList.addAll(authorizeHeaders);

        // 如果没有 bearer token，那么添加当前 client token 到 Authorization 中
        if (authorizeList.stream().noneMatch(value -> StringUtils.startsWithIgnoreCase(value, OAUTH2_BEARER_TYPE))){
            final String microAuthHeader = String.format("%s %s", OAUTH2_BEARER_TYPE, clientTokenUtils.getAccessToken());
            log.info(">>> OAuth2FeignRequestInterceptor - 设置 Client Token: {}", microAuthHeader);
            authorizeList.add(microAuthHeader);
            template.header(HttpHeaders.AUTHORIZATION, authorizeList);
        }

        log.info(">>> OAuth2FeignRequestInterceptor - Authorization headers={}", authorizeList);
    }

    /**
     * 转发 header 到 RequestTemplate
     */
    private void relayHeader(RequestTemplate template){
        HttpServletRequest request = RequestUtils.getRequest();
        if (request == null){
            return;
        }

        // 传递所有header
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
