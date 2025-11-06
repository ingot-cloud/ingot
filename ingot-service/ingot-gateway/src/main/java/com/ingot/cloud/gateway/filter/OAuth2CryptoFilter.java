package com.ingot.cloud.gateway.filter;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.utils.Try;
import com.ingot.framework.crypto.InCryptoProperties;
import com.ingot.framework.crypto.model.CryptoType;
import com.ingot.framework.crypto.utils.CryptoUtils;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : 处理OAuth2相关端点，解密需要加密的参数.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 8:37 下午.</p>
 */
@Slf4j
@Component
@SuppressWarnings("all")
@RequiredArgsConstructor
public class OAuth2CryptoFilter extends AbstractGatewayFilterFactory {
    private static final String DecryptFailureValue = "_ingot@DecryptionFailureValue_";
    private static final List<String> DecryptKeys = ListUtil.list(false,
            "username", "password", "client_id", "scope");
    private static final List<String> DecoderURIs = ListUtil.list(false,
            SecurityConstants.TOKEN_ENDPOINT_URI,
            SecurityConstants.PRE_AUTHORIZE_URI,
            SecurityConstants.AUTHORIZE_URI);

    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();
    private final InCryptoProperties properties;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            log.info("[TokenPasswordDecoderFilter] - path={}", path);

            if (StrUtil.isEmpty(getAesKey())) {
                return chain.filter(exchange);
            }

            // 只拦截Token端点和预授权端点
            if (DecoderURIs.stream().noneMatch(item -> StrUtil.containsAnyIgnoreCase(path, item))) {
                return chain.filter(exchange);
            }

            // 处理查询参数
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            boolean hasQueryParamsToDecrypt = queryParams.keySet().stream()
                    .anyMatch(DecryptKeys::contains);

            // 如果有查询参数需要解密
            if (hasQueryParamsToDecrypt) {
                URI newUri = decryptQueryParams(request.getURI(), queryParams);
                ServerHttpRequest modifiedRequest = request.mutate()
                        .uri(newUri)
                        .build();
                exchange = exchange.mutate().request(modifiedRequest).build();
            }

            // 处理Body参数
            MediaType contentType = request.getHeaders().getContentType();
            if (contentType != null && contentType.includes(MediaType.APPLICATION_FORM_URLENCODED)) {
                return decryptBodyParams(exchange, chain);
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 解密查询参数
     */
    private URI decryptQueryParams(URI originalUri, MultiValueMap<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(originalUri);
        builder.replaceQueryParams(null); // 清空原有查询参数

        queryParams.forEach((key, values) -> {
            if (DecryptKeys.contains(key) && values != null && !values.isEmpty()) {
                // 解密参数值
                String encryptedValue = values.get(0);
                String decryptedValue = Try.of(() -> CryptoUtils.decryptAES(getAesKey(), encryptedValue))
                        .getOrElse(DecryptFailureValue);
                builder.queryParam(key, decryptedValue);
                log.debug("[TokenPasswordDecoderFilter] - 解密查询参数: key={}", key);
            } else {
                // 保持原值
                values.forEach(value -> builder.queryParam(key, value));
            }
        });

        return builder.encode().build().toUri();
    }

    /**
     * 解密Body参数
     */
    private Mono<Void> decryptBodyParams(ServerWebExchange exchange,
                                         org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
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
    }

    private Function decryptAES() {
        return s -> {
            Map<String, String> inParamsMap = HttpUtil.decodeParamMap((String) s, CharsetUtil.CHARSET_UTF_8);

            inParamsMap.keySet()
                    .stream()
                    .filter(key -> DecryptKeys.contains(key))
                    .forEach(key -> {
                        String value = inParamsMap.get(key);
                        inParamsMap.put(key, Try.of(() -> CryptoUtils.decryptAES(getAesKey(), value))
                                .getOrElse(DecryptFailureValue));
                        log.debug("[TokenPasswordDecoderFilter] - 解密Body参数: key={}", key);
                    });

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

    private String getAesKey() {
        return properties.getSecretKeys().get(CryptoType.AES.getValue());
    }
}
