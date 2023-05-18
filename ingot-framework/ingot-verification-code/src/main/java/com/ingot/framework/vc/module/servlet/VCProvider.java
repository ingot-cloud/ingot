package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VCType;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : VCProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 10:44 AM.</p>
 */
public interface VCProvider {

    /**
     * 创建验证码
     *
     * @param request   {@link ServletWebRequest}
     * @param generator {@link VCGenerator}
     */
    void create(ServletWebRequest request, VCGenerator generator);

    /**
     * 校验验证码
     *
     * @param request {@link ServletWebRequest}
     * @param type    {@link VCType}
     */
    void validate(ServletWebRequest request, VCType type);
}
