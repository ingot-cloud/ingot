package com.ingot.framework.vc.module.servlet;

import java.util.Map;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : DefaultVCProviderManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 11:02 AM.</p>
 */
@RequiredArgsConstructor
public class DefaultVCProviderManager implements VCProviderManager {
    private final Map<String, VCProvider> providerMap;
    private final Map<String, VCGenerator> generatorMap;

    @Override
    public void create(VCType type, ServletWebRequest request) {
        VCProvider provider = getProvider(type);
        VCGenerator generator = getGenerator(type);
        provider.create(request, generator);
    }

    @Override
    public void validate(VCType type, ServletWebRequest request) {
        VCProvider provider = getProvider(type);
        provider.validate(request, type);
    }

    private VCProvider getProvider(VCType type) {
        String beanName = type.getProviderBeanName();
        VCProvider provider = providerMap.get(beanName);
        InnerCheck.check(provider != null, "vc.common.typeError");
        return provider;
    }

    private VCGenerator getGenerator(VCType type) {
        String beanName = type.getGeneratorBeanName();
        VCGenerator generator = generatorMap.get(beanName);
        InnerCheck.check(generator != null, "vc.common.typeError");
        return generator;
    }
}
