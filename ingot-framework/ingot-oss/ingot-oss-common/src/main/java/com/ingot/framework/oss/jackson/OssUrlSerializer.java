package com.ingot.framework.oss.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.oss.common.OssObjectInfo;
import com.ingot.framework.oss.common.OssPathParser;
import com.ingot.framework.oss.common.OssUrl;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * <p>Description  : MinioURLSerializer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 11:25.</p>
 */
@RequiredArgsConstructor
public class OssUrlSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private final OssService ossService;
    @Setter
    private int expireSeconds;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeNull();
            return;
        }

        try {
            // 解析 bucket/objectName
            OssObjectInfo info = OssPathParser.parse(value);
            String url = info.bucket() + "/" + info.objectName();
            // 生成临时URL
            String presignedUrl = expireSeconds > 0 ?
                    ossService.getObjectURL(url, expireSeconds) : ossService.getObjectURL(url);
            gen.writeString(presignedUrl);
        } catch (Exception e) {
            // 如果失败则原样返回
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            OssUrl ann = property.getAnnotation(OssUrl.class);
            if (ann == null) {
                ann = property.getContextAnnotation(OssUrl.class);
            }
            if (ann != null) {
                // 动态带入 expireSeconds
                OssUrlSerializer serializer = new OssUrlSerializer(ossService);
                serializer.setExpireSeconds(ann.expireSeconds());
                return serializer;
            }
        }
        return this;
    }
}
