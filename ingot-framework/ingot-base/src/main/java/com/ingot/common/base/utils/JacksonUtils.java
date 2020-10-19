package com.ingot.common.base.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.Writer;

/**
 * <p>Description  : JacksonUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-11-06.</p>
 * <p>Time         : 17:02.</p>
 */
@Slf4j
@UtilityClass
public class JacksonUtils {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link ObjectMapper#writeValue(Writer, Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     */
    public String writeValueAsString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Method to deserialize JSON content from given JSON content String.
     */
    public <T> T readValue(String jsonStr, Class<T> cls) {
        try {
            return mapper.readValue(jsonStr, cls);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
