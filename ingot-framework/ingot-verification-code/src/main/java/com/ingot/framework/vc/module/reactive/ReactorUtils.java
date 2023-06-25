package com.ingot.framework.vc.module.reactive;

import com.ingot.framework.core.model.support.R;
import com.ingot.framework.vc.common.VCConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : ReactorUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 2:12 PM.</p>
 */
public class ReactorUtils {

    /**
     * 获取接收者
     *
     * @param request {@link ServerRequest}
     * @return 接受者信息
     */
    public static String getReceiver(ServerRequest request) {
        return request.queryParam(VCConstants.QUERY_PARAMS_RECEIVER).orElse("");
    }

    /**
     * 获取接收者
     *
     * @param request {@link ServerHttpRequest}
     * @return 接受者信息
     */
    public static String getReceiver(ServerHttpRequest request) {
        return request.getQueryParams().getFirst(VCConstants.QUERY_PARAMS_RECEIVER);
    }

    /**
     * 获取验证码
     *
     * @param request {@link ServerRequest}
     * @return 验证码
     */
    public static String getCode(ServerRequest request) {
        return request.queryParam(VCConstants.QUERY_PARAMS_RECEIVER).orElse("");
    }

    /**
     * 获取验证码
     *
     * @param request {@link ServerHttpRequest}
     * @return 验证码
     */
    public static String getCode(ServerHttpRequest request) {
        return request.getQueryParams().getFirst(VCConstants.QUERY_PARAMS_CODE);
    }

    /**
     * 默认发送成功响应
     *
     * @return ServerResponse stream
     */
    public static Mono<ServerResponse> defaultSendSuccess() {
        return successResponse(R.ok(Boolean.TRUE));
    }

    /**
     * 成功响应
     *
     * @param data response data
     * @return ServerResponse stream
     */
    public static <T> Mono<ServerResponse> successResponse(T data) {
        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(data));
    }
}
