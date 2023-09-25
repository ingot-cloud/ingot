package com.ingot.framework.crypto.jackson;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.ingot.framework.crypto.annotation.IngotDecrypt;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.model.CryptoType;
import com.ingot.framework.crypto.utils.CryptoUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>Description  : CryptoDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 3:10 PM.</p>
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class CryptoDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {
    private CryptoType type;
    private String key;

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if (p == null || StrUtil.isEmpty(p.getText())) {
            return null;
        }

        CryptoInfoRecord record = new CryptoInfoRecord(type, key);
        return switch (type) {
            case AES, RSA -> StrUtil.str(
                    CryptoUtils.decrypt(p.getBinaryValue(), record),
                    StandardCharsets.UTF_8);
        };
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
        if (beanProperty != null) {
            if (Objects.equals(beanProperty.getType().getRawClass(), String.class)) {
                IngotDecrypt crypto = beanProperty.getAnnotation(IngotDecrypt.class);
                if (crypto == null) {
                    crypto = beanProperty.getContextAnnotation(IngotDecrypt.class);
                }
                if (crypto != null) {
                    return new CryptoDeserializer(crypto.value(), crypto.secretKey());
                }
            }
            return ctxt.findContextualValueDeserializer(beanProperty.getType(), beanProperty);
        }
        return null;
    }
}