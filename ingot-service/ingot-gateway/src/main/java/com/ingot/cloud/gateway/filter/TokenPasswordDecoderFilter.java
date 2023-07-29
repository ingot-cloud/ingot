package com.ingot.cloud.gateway.filter;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.core.utils.AESUtils;
import com.ingot.framework.core.utils.crypto.IngotCryptoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>Description  : TokenPasswordDecoderFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 8:37 下午.</p>
 */
@Slf4j
@Component
@SuppressWarnings("all")
@RequiredArgsConstructor
public class TokenPasswordDecoderFilter extends AbstractGatewayFilterFactory {
    private static final String PASSWORD = "password";
    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();
    private final IngotCryptoProperties ingotCryptoProperties;

    private final List<String> DecoderURIs = ListUtil.list(false,
            SecurityConstants.TOKEN_ENDPOINT_URI,
            SecurityConstants.PRE_AUTHORIZE_URI);

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            log.info("[TokenPasswordDecoderFilter] - path={}", path);

            if (StrUtil.isEmpty(ingotCryptoProperties.getAesKey())) {
                return chain.filter(exchange);
            }

            // 只拦截Token端点和预授权端点
            if (DecoderURIs.stream().noneMatch(item -> StrUtil.containsAnyIgnoreCase(path, item))) {
                return chain.filter(exchange);
            }

            Class inClass = String.class;
            Class outClass = String.class;
            ServerRequest serverRequest = ServerRequest.create(exchange, messageReaders);

            // 解密生成新的报文
            Mono<?> modifiedBody = serverRequest.bodyToMono(inClass).flatMap(decryptAES());

            BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, outClass);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(exchange.getRequest().getHeaders());
            headers.remove(HttpHeaders.CONTENT_LENGTH);

            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
            return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
                ServerHttpRequest decorator = decorate(exchange, headers, outputMessage);
                return chain.filter(exchange.mutate().request(decorator).build());
            }));
        };
    }

    private Function decryptAES() {
        return s -> {
            Map<String, String> inParamsMap = HttpUtil.decodeParamMap((String) s, CharsetUtil.CHARSET_UTF_8);
            // 只处理password字段
            if (inParamsMap.containsKey(PASSWORD)) {
                String password = AESUtils.decryptAES(ingotCryptoProperties.getAesKey(), inParamsMap.get(PASSWORD));
                // 返回修改后报文字符
                inParamsMap.put(PASSWORD, password);
            }

            return Mono.just(HttpUtil.toParams(inParamsMap, Charset.defaultCharset(), true));
        };
    }

    private ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers,
                                                CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }
}
