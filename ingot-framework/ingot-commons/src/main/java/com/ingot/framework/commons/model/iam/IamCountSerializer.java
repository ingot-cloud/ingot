package com.ingot.framework.commons.model.iam;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * <p>将 IAM 可见记录统计输出为 JSON 数字，覆盖应用针对 Long 标识符的字符串序列化规则。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamCountSerializer extends JsonSerializer<Long> {
    /**
     * 写入经过权限过滤的统计值；空值由 Jackson 的空值策略处理。
     *
     * @param value 非空统计值
     * @param generator JSON 输出
     * @param serializers 当前序列化上下文
     * @throws IOException 输出失败时抛出
     */
    @Override
    public void serialize(Long value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeNumber(value);
    }
}
