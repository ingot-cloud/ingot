package com.ingot.framework.vc.common;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import com.ingot.framework.vc.module.servlet.VCProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 3:55 PM.</p>
 */
@Slf4j
public class Utils {

    /**
     * 获取Processor
     *
     * @param type         {@link VCType}
     * @param processorMap processor映射表
     * @return {@link VCProvider}
     */
    public static VCProcessor getProcessor(VCType type, Map<String, VCProcessor> processorMap) {
        String beanName = type.getProcessorBeanName();
        VCProcessor processor = processorMap.get(beanName);
        InnerCheck.check(processor != null, "vc.common.typeError");
        return processor;
    }

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
     * @return {@link VCPreChecker}
     */
    public static VCPreChecker getSendChecker(VCType type, Map<String, VCPreChecker> sendCheckerMap) {
        String beanName = type.getSendCheckerBeanName();
        VCPreChecker checker = sendCheckerMap.get(beanName);
        if (checker == null) {
            return DefaultPreChecker.DEFAULT;
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
        throwVCException(VCErrorCode.Check, code);
    }

    /**
     * 验证码异常
     *
     * @param statusCode  {@link VCErrorCode}
     * @param messageCode message code
     */
    public static void throwVCException(VCErrorCode statusCode, String messageCode) {
        throw new VCException(statusCode,
                InVCMessageSource.getAccessor()
                        .getMessage(messageCode));
    }

    /**
     * 验证码异常
     *
     * @param statusCode  {@link VCErrorCode}
     * @param messageCode message code
     * @param args        message args
     */
    public static void throwVCException(VCErrorCode statusCode, String messageCode, Object[] args) {
        throw new VCException(statusCode,
                InVCMessageSource.getAccessor()
                        .getMessage(messageCode, args));
    }
}
