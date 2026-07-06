package com.ingot.framework.crypto.jackson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.ingot.framework.crypto.annotation.InFieldDecrypt;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.model.CryptoType;
import com.ingot.framework.crypto.utils.CryptoUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : CryptoDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 3:10 PM.</p>
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class CryptoDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
    private CryptoType type;
    private String key;
    private JavaType targetType;

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if (p == null || StrUtil.isEmpty(p.getText())) {
            return null;
        }

        CryptoInfoRecord record = new CryptoInfoRecord(type, key);
        try {
            // 先解密得到字符串
            String decryptedStr = switch (type) {
                case AES, AES_GCM, RSA -> StrUtil.str(
                        CryptoUtils.decrypt(p.getText().getBytes(StandardCharsets.UTF_8), record),
                        StandardCharsets.UTF_8
                ).trim();
            };

            if (targetType == null) {
                return decryptedStr;
            }

            // 如果目标类型是 String，直接返回
            if (targetType.getRawClass() == String.class) {
                return decryptedStr;
            }

            // 将解密后的字符串转换为目标类型
            JsonParser newParser = ctxt.getParser().getCodec().getFactory().createParser(decryptedStr);
            JsonDeserializer<Object> deserializer = ctxt.findContextualValueDeserializer(targetType, null);
            newParser.nextToken();
            return deserializer.deserialize(newParser, ctxt);
        } catch (Exception e) {
            log.error("[CryptoDeserializer] - 解密失败", e);
            return null;
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
        if (beanProperty != null) {
            InFieldDecrypt crypto = beanProperty.getAnnotation(InFieldDecrypt.class);
            if (crypto == null) {
                crypto = beanProperty.getContextAnnotation(InFieldDecrypt.class);
            }
            if (crypto != null) {
                // 保存目标类型信息
                JavaType fieldType = beanProperty.getType();
                return new CryptoDeserializer(crypto.value(), crypto.secretKey(), fieldType);
            }
            return ctxt.findContextualValueDeserializer(beanProperty.getType(), beanProperty);
        }
        return null;
    }
}