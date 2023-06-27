package com.ingot.framework.vc.module.reactive;

import cn.hutool.core.collection.CollUtil;
import com.ingot.framework.vc.common.ServerWebExchangeMatcher;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

/**
 * <p>Description  : VCWebFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 1:56 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VCWebFilter implements WebFilter {
    private final VCProcessorManager processorManager;
    private final VCVerifyResolver verifyResolver;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,
                             @NonNull WebFilterChain chain) {
        log.info("[VCWebFilter] - request path={}", exchange.getRequest().getPath());

        List<VCType> typeList = verifyResolver.getTypeList();
        List<ServerWebExchangeMatcher> requestMatcherList = verifyResolver.getRequestMatcherList();
        if (CollUtil.isEmpty(typeList)) {
            return chain.filter(exchange);
        }

        return Flux.zip(Flux.fromIterable(typeList), Flux.fromIterable(requestMatcherList))
                .filterWhen(item -> item.getT2().matches(exchange))
                .map(Tuple2::getT1)
                .elementAt(0, VCType.DEFAULT)
                .flatMap(item -> {
                    if (item == VCType.DEFAULT) {
                        return chain.filter(exchange);
                    }
                    return processorManager.checkOnly(item, exchange, chain);
                });
    }
}
