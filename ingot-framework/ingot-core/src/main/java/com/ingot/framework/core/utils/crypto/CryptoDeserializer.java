package com.ingot.framework.core.utils.crypto;

import java.io.IOException;
import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.core.utils.AESUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : CryptoDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/30.</p>
 * <p>Time         : 5:55 PM.</p>
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
        switch (type) {
            case AES:
                if (StrUtil.isEmpty(key)) {
                    key = SpringContextHolder.getBean(IngotCryptoProperties.class).getAesKey();
                }
                if (StrUtil.isEmpty(key)) {
                    log.warn("[CryptoDeserializer] 请配置ingot.crypto.aesKey");
                    return p.getText();
                }
                return AESUtils.decryptAES(key, p.getText());
            default:
                return p.getText();
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
        if (beanProperty != null) {
            if (Objects.equals(beanProperty.getType().getRawClass(), String.class)) {
                IngotCrypto crypto = beanProperty.getAnnotation(IngotCrypto.class);
                if (crypto == null) {
                    crypto = beanProperty.getContextAnnotation(IngotCrypto.class);
                }
                if (crypto != null) {
                    return new CryptoDeserializer(crypto.type(), crypto.key());
                }
            }
            return ctxt.findContextualValueDeserializer(beanProperty.getType(), beanProperty);
        }
        return null;
    }
}
