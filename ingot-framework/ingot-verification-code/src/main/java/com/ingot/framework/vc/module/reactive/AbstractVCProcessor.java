package com.ingot.framework.vc.module.reactive;

import com.ingot.framework.core.model.support.R;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : AbstractVCProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 2:30 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractVCProcessor implements VCProcessor {
    private final VCRepository repository;

    /**
     * 发送验证码
     *
     * @param request      {@link ServerRequest request}
     * @param validateCode {@link VC}
     * @return ServerResponse stream
     */
    protected abstract Mono<ServerResponse> send(ServerRequest request, VC validateCode);

    @Override
    public Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator) {
        VC code = generator.generate();
        return send(request, code).flatMap(response -> {
            try {
                repository.save(ReactorUtils.getReceiver(request), code);
            } catch (Exception e) {
                return Mono.error(e);
            }
            return Mono.just(response);
        });
    }

    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            VC codeInCache = repository.get(ReactorUtils.getReceiver(request), type);
            String codeInRequest = ReactorUtils.getCode(request);
            Utils.checkCode(codeInRequest, codeInCache);
            return chain.filter(exchange);
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<ServerResponse> check(VCType type, ServerRequest request) {
        try {
            VC codeInCache = repository.get(ReactorUtils.getReceiver(request), type);
            String codeInRequest = ReactorUtils.getCode(request);
            Utils.checkCode(codeInRequest, codeInCache);
            return ReactorUtils.successResponse(R.ok(Boolean.TRUE));
        } catch (VCException e) {
            return Mono.error(e);
        }
    }
}
