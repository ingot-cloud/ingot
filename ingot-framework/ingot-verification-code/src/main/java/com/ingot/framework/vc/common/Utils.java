package com.ingot.framework.vc.common;

import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.module.servlet.VCProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 3:55 PM.</p>
 */
@Slf4j
public class Utils {

    /**
     * 获取Provider
     *
     * @param type        {@link VCType}
     * @param providerMap provider映射表
     * @return {@link VCProvider}
     */
    public static VCProvider getProvider(VCType type, Map<String, VCProvider> providerMap) {
        String beanName = type.getProviderBeanName();
        VCProvider provider = providerMap.get(beanName);
        InnerCheck.check(provider != null, "vc.common.typeError");
        return provider;
    }

    /**
     * 获取生成器
     *
     * @param type         {@link VCType}
     * @param generatorMap generator映射表
     * @return {@link VCGenerator}
     */
    public static VCGenerator getGenerator(VCType type, Map<String, VCGenerator> generatorMap) {
        String beanName = type.getGeneratorBeanName();
        VCGenerator generator = generatorMap.get(beanName);
        InnerCheck.check(generator != null, "vc.common.typeError");
        return generator;
    }

    /**
     * 获取发送检测器
     *
     * @param type           {@link VCType}
     * @param sendCheckerMap checker映射表
     * @return {@link VCSendChecker}
     */
    public static VCSendChecker getSendChecker(VCType type, Map<String, VCSendChecker> sendCheckerMap) {
        String beanName = type.getSendCheckerBeanName();
        VCSendChecker checker = sendCheckerMap.get(beanName);
        if (checker == null) {
            return DefaultSendChecker.DEFAULT;
        }
        return checker;
    }

    /**
     * 检查验证码通用逻辑
     *
     * @param codeInRequest 请求中的验证码
     * @param codeInCache   缓存中的验证码
     */
    public static void checkCode(String codeInRequest, VC codeInCache) {
        if (StrUtil.isBlank(codeInRequest)) {
            throwCheckException("vc.check.notNull");
        }

        if (codeInCache == null || codeInCache.isExpired()) {
            throwCheckException("vc.check.expired");
        }

        if (!StrUtil.equals(codeInCache.getValue(), codeInRequest)) {
            throwCheckException("vc.check.notAvailable");
        }
    }

    /**
     * 检查异常
     *
     * @param code message code
     */
    public static void throwCheckException(String code) {
        throw new VCException(VCStatusCode.Check,
                IngotVCMessageSource.getAccessor()
                        .getMessage(code));
    }
}
