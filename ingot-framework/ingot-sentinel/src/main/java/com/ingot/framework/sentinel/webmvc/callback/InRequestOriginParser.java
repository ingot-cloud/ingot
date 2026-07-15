package com.ingot.framework.sentinel.webmvc.callback;

import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.utils.WebUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>自定义 {@link RequestOriginParser}。</p>
 *
 * <p>解析顺序见 {@link HeaderConstants#REQUEST_SOURCE_IP_HEADERS}（经 {@link WebUtil#getClientIP}），
 * 无法解析时返回 {@code unknown}。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public class InRequestOriginParser implements RequestOriginParser {

    private static final String UNKNOWN = "unknown";

    @Override
    public String parseOrigin(HttpServletRequest request) {
        String ip = WebUtil.getClientIP(request);
        return StrUtil.isNotBlank(ip) ? ip : UNKNOWN;
    }
}
