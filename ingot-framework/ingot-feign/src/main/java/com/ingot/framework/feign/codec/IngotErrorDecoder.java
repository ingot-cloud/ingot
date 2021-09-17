package com.ingot.framework.feign.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.feign.exception.IngotFeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;

/**
 * <p>Description  : IngotErrorDecoder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/31.</p>
 * <p>Time         : 5:23 下午.</p>
 */
@Slf4j
public class IngotErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultErrorDecoder = new ErrorDecoder.Default();

    @SuppressWarnings("rawtypes")
    @Override
    public Exception decode(String methodKey, Response response) {
        ObjectMapper mapper = new ObjectMapper();
        log.info("IngotErrorDecoder - start decode, methodKey={}", methodKey);
        try {
            HashMap map = mapper.readValue(response.body().asInputStream(), HashMap.class);
            String code = (String) map.get(R.CODE);
            String message = (String) map.get(R.MESSAGE);
            log.info("IngotErrorDecoder - map={}", map);
            return new IngotFeignException(code, message);
        } catch (IOException e) {
            log.error("IngotErrorDecoder - IOException, message={}, e={}", e.getMessage(), e);
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}
