package com.ingot.framework.vc.module.servlet;

import java.util.Map;

import com.ingot.framework.commons.utils.WebUtil;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.Utils;
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
    private final Map<String, VCPreChecker> checkerMap;

    @Override
    public void create(VCType type, ServletWebRequest request) {
        VCProvider provider = Utils.getProvider(type, providerMap);
        VCGenerator generator = Utils.getGenerator(type, generatorMap);
        VCPreChecker checker = Utils.getSendChecker(type, checkerMap);

        String receiver = ServletUtils.getReceiver(request);
        String remoteIP = WebUtil.getRemoteIP(request.getRequest());
        checker.beforeSend(receiver, remoteIP);
        provider.create(request, generator);
    }

    @Override
    public void checkOnly(VCType type, ServletWebRequest request) {
        VCProvider provider = Utils.getProvider(type, providerMap);
        VCPreChecker checker = Utils.getSendChecker(type, checkerMap);

        String remoteIP = WebUtil.getRemoteIP(request.getRequest());
        checker.beforeCheck(remoteIP);
        provider.checkOnly(request, type);
    }

    @Override
    public void check(VCType type, ServletWebRequest request) {
        VCProvider provider = Utils.getProvider(type, providerMap);
        VCPreChecker checker = Utils.getSendChecker(type, checkerMap);

        String remoteIP = WebUtil.getRemoteIP(request.getRequest());
        checker.beforeCheck(remoteIP);
        provider.check(request, type);
    }
}
