package com.ingot.framework.crypto.jackson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ingot.framework.crypto.annotation.InFieldEncrypt;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.model.CryptoType;
import com.ingot.framework.crypto.utils.CryptoUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : CryptoSerializer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/26.</p>
 * <p>Time         : 10:00.</p>
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class CryptoSerializer extends JsonSerializer<Object> implements ContextualSerializer {
    private CryptoType type;
    private String key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        CryptoInfoRecord record = new CryptoInfoRecord(type, key);
        try {
            String en = switch (type) {
                case AES, AES_GCM, RSA -> {
                    String enValue;
                    if (value instanceof String) {
                        enValue = value.toString();
                    } else {
                        enValue = objectMapper.writeValueAsString(value);
                    }
                    yield StrUtil.str(
                            CryptoUtils.encrypt(enValue.getBytes(StandardCharsets.UTF_8), record),
                            StandardCharsets.UTF_8
                    ).trim();
                }
            };
            if (StrUtil.isEmpty(en)) {
                gen.writeNull();
                return;
            }
            gen.writeString(en);
        } catch (Exception e) {
            // 如果失败则原样返回指定内容
            gen.writeString(CryptoUtils.ENCRYPT_FAIL_CONTENT);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            InFieldEncrypt ann = property.getAnnotation(InFieldEncrypt.class);
            if (ann == null) {
                ann = property.getContextAnnotation(InFieldEncrypt.class);
            }
            if (ann != null) {
                return new CryptoSerializer(ann.value(), ann.secretKey());
            }
        }
        return this;
    }
}
