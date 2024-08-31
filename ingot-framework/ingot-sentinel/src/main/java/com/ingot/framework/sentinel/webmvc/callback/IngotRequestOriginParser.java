package com.ingot.framework.sentinel.webmvc.callback;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;


/**
 * <p>Description  : IngotRequestOriginParser.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 4:00 下午.</p>
 */
public class IngotRequestOriginParser implements RequestOriginParser {

    private static final String ALLOW = "Allow";

    /**
     * 解析请求IP
     *
     * @param request HTTP request
     * @return parsed origin
     */
    @Override
    public String parseOrigin(HttpServletRequest request) {
        return request.getHeader(ALLOW);
    }
}
