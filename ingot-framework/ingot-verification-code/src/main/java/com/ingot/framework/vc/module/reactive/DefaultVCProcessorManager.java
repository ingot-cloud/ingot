package com.ingot.framework.vc.module.reactive;

import com.ingot.framework.core.utils.reactive.WebUtils;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * <p>Description  : DefaultVCProcessorManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 2:07 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultVCProcessorManager implements VCProcessorManager {
    private final Map<String, VCProcessor> processorMap;
    private final Map<String, VCGenerator> generatorMap;
    private final Map<String, VCSendChecker> checkerMap;

    @Override
    public Mono<ServerResponse> handle(VCType type, ServerRequest request) {
        try {
            VCProcessor processor = Utils.getProcessor(type, processorMap);
            VCGenerator generator = Utils.getGenerator(type, generatorMap);
            VCSendChecker checker = Utils.getSendChecker(type, checkerMap);

            String receiver = ReactorUtils.getReceiver(request);
            String remoteIP = WebUtils.getRemoteIP(request);

            checker.check(receiver, remoteIP);
            return processor.handle(request, generator);
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        VCProcessor processor = Utils.getProcessor(type, processorMap);
        return processor.checkOnly(type, exchange, chain);
    }

    @Override
    public Mono<ServerResponse> check(VCType type, ServerRequest request) {
        VCProcessor processor = Utils.getProcessor(type, processorMap);
        return processor.check(type, request);
    }
}
