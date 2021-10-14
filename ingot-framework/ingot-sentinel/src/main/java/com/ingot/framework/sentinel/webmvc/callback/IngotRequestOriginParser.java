package com.ingot.framework.sentinel.webmvc.callback;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.RequestOriginParser;

/**
 * <p>Description  : IngotRequestOriginParser.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 4:00 下午.</p>
 */
public class IngotRequestOriginParser implements RequestOriginParser {

    /**
     * 解析请求IP
     *
     * @param request HTTP request
     * @return parsed origin
     */
    @Override
    public String parseOrigin(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
