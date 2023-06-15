package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.common.VCException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Description  : VCFailureHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 11:03 AM.</p>
 */
public interface VCFailureHandler {

    /**
     * 验证失败处理
     *
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     * @param exception {@link VCException}
     */
    void onFailure(HttpServletRequest request, HttpServletResponse response,
                   VCException exception) throws IOException, ServletException;
}
