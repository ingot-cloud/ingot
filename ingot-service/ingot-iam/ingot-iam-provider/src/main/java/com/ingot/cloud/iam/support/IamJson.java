package com.ingot.cloud.iam.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>把目录 JSON 列与审计安全差异序列化为稳定文本，失败时关闭为参数错误。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamJson {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final String EMPTY_OBJECT = "{}";
    private static final String EMPTY_ARRAY = "[]";

    private IamJson() {
    }

    /**
     * 序列化对象；空引用写成 JSON 对象。
     *
     * @param value 待写入值
     * @return JSON 文本
     */
    public static String object(Object value) {
        if (value == null) {
            return EMPTY_OBJECT;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    /**
     * 序列化数组；空引用写成空数组。
     *
     * @param value 待写入集合
     * @return JSON 文本
     */
    public static String array(Object value) {
        if (value == null) {
            return EMPTY_ARRAY;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    /**
     * 读取 JSON 列。
     *
     * @param json 列文本
     * @param type 目标类型
     * @param <T> 反序列化类型
     * @return 解析结果；空列返回 null
     */
    public static <T> T read(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
