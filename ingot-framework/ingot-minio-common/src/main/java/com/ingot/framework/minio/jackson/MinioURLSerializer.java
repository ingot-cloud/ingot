package com.ingot.framework.minio.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ingot.framework.core.oss.OssService;
import com.ingot.framework.minio.common.MinioObjectInfo;
import com.ingot.framework.minio.common.MinioPathParser;
import com.ingot.framework.minio.common.MinioURL;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * <p>Description  : MinioURLSerializer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 11:25.</p>
 */
@RequiredArgsConstructor
public class MinioURLSerializer extends JsonSerializer<String> implements ContextualSerializer {
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
            MinioObjectInfo info = MinioPathParser.parse(value);
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
            MinioURL ann = property.getAnnotation(MinioURL.class);
            if (ann == null) {
                ann = property.getContextAnnotation(MinioURL.class);
            }
            if (ann != null) {
                // 动态带入 expireSeconds
                MinioURLSerializer serializer = new MinioURLSerializer(ossService);
                serializer.setExpireSeconds(ann.expireSeconds());
                return serializer;
            }
        }
        return this;
    }
}
