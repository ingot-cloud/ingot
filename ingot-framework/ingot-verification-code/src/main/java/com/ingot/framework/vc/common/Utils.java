package com.ingot.framework.vc.common;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 3:55 PM.</p>
 */
public class Utils {

    /**
     * 检查验证码通用逻辑
     *
     * @param codeInRequest 请求中的验证码
     * @param codeInCache   缓存中的验证码
     */
    public static void checkCode(String codeInRequest, VC codeInCache) {
        if (StrUtil.isBlank(codeInRequest)) {
            throw new VCException(VCStatusCode.Check,
                    IngotVCMessageSource.getAccessor()
                            .getMessage("vc.check.notNull"));
        }

        if (codeInCache == null || codeInCache.isExpired()) {
            throw new VCException(VCStatusCode.Check,
                    IngotVCMessageSource.getAccessor()
                            .getMessage("vc.check.expired"));
        }

        if (!StrUtil.equals(codeInCache.getCode(), codeInRequest)) {
            throw new VCException(VCStatusCode.Check,
                    IngotVCMessageSource.getAccessor()
                            .getMessage("vc.check.notAvailable"));
        }
    }
}
