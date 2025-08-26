package com.ingot.framework.crypto.jackson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
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
public class CryptoSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private CryptoType type;
    private String key;


    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeNull();
            return;
        }
        CryptoInfoRecord record = new CryptoInfoRecord(type, key);
        try {
            String en = switch (type) {
                case AES, AES_GCM, RSA -> StrUtil.str(
                        CryptoUtils.encrypt(value.getBytes(StandardCharsets.UTF_8), record),
                        StandardCharsets.UTF_8
                ).trim();
            };
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
