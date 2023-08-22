package com.ingot.framework.vc.module.servlet;

import cn.hutool.core.util.CharsetUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.vc.common.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 2:27 PM.</p>
 */
@Slf4j
public class ServletUtils {

    /**
     * 获取接收者
     *
     * @param request {@link ServletWebRequest}
     * @return 接收者信息
     */
    public static String getReceiver(ServletWebRequest request) {
        return getFromRequest(request, VCConstants.QUERY_PARAMS_RECEIVER);
    }

    /**
     * 获取验证码
     *
     * @param request {@link ServletWebRequest}
     * @return 验证码
     */
    public static String getCode(ServletWebRequest request) {
        return getFromRequest(request, VCConstants.QUERY_PARAMS_CODE);
    }

    /**
     * 获取参数
     *
     * @param request {@link ServletWebRequest}
     * @param key     参数
     * @return 参数
     */
    public static String getFromRequest(ServletWebRequest request, String key) {
        try {
            return ServletRequestUtils.getStringParameter(request.getRequest(), key);
        } catch (Exception e) {
            log.error("[验证码] - getFromRequest 异常", e);
            throw new VCException(IngotVCMessageSource.getAccessor()
                    .getMessage("vc.common.illegalArgument",
                            new String[]{key}));
        }
    }

    /**
     * 默认发送成功处理
     *
     * @param request      {@link ServletWebRequest}
     * @param objectMapper {@link ObjectMapper}
     * @throws Exception error
     */
    public static void defaultSendSuccess(ServletWebRequest request,
                                          ObjectMapper objectMapper) throws Exception {
        successResponse(request, objectMapper, R.ok(Boolean.TRUE));
    }

    /**
     * 成功相应
     *
     * @param request      {@link ServletWebRequest}
     * @param objectMapper {@link ObjectMapper}
     * @param data         response
     * @throws Exception error
     */
    public static void successResponse(ServletWebRequest request,
                                       ObjectMapper objectMapper,
                                       Object data) throws Exception {
        // 响应结果
        HttpServletResponse response = request.getResponse();
        assert response != null;
        response.setCharacterEncoding(CharsetUtil.UTF_8);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(data));
        response.flushBuffer();
    }

}
