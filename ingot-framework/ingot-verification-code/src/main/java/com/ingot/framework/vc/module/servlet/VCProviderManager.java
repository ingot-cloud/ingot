package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.common.VCType;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : VCProviderManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 10:59 AM.</p>
 */
public interface VCProviderManager {

    /**
     * 创建验证码
     *
     * @param type    {@link VCType}
     * @param request {@link ServletWebRequest}
     */
    void create(VCType type, ServletWebRequest request);

    /**
     * 仅做检查，如果检查失败抛出异常
     *
     * @param type    {@link VCType}
     * @param request {@link ServletWebRequest}
     */
    void checkOnly(VCType type, ServletWebRequest request);

    /**
     * 校验验证码返回响应结果
     *
     * @param type    {@link VCType}
     * @param request {@link ServletWebRequest}
     */
    void check(VCType type, ServletWebRequest request);
}
