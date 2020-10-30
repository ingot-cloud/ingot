package com.ingot.framework.feign;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.base.status.BaseStatusCode;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;


/**
 * <p>Description  : FeignErrorDecoder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/14.</p>
 * <p>Time         : 下午5:03.</p>
 */
@Slf4j
public class OAuth2FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultErrorDecoder = new Default();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override public Exception decode(String methodKey, Response response) {
        log.info(">>> OAuth2FeignErrorDecoder - start decode, methodKey={}", methodKey);
        try {
            HashMap map = mapper.readValue(response.body().asInputStream(), HashMap.class);
            String code = (String) map.get("code");
            String message = (String) map.get("message");
            log.info(">>> OAuth2FeignErrorDecoder - map={}", map);
            if (code != null) {
                if (StrUtil.equals(code, BaseStatusCode.ILLEGAL_REQUEST_PARAMS.code())) {
                    throw new IllegalArgumentException(message);
                } else {
                    throw new BaseException(code, message);
                }
            }
        } catch (IOException e) {
            log.error(">>> OAuth2FeignErrorDecoder - IOException, message={}, e={}", e.getMessage(), e);
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}
