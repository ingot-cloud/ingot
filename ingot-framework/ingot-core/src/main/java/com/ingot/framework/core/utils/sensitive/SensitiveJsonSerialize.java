package com.ingot.framework.core.utils.sensitive;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * <p>Description  : SensitiveJsonSerialize.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/1.</p>
 * <p>Time         : 5:45 PM.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveJsonSerialize extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveMode mode;

    private Integer prefixPlaintextLength;

    private Integer suffixPlaintextLength;

    private String maskCode;

    @Override
    public void serialize(final String origin, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        switch (mode) {
            case CHINESE_NAME:
                jsonGenerator.writeString(SensitiveUtils.chineseName(origin));
                break;
            case ID_CARD:
                jsonGenerator.writeString(SensitiveUtils.idCardNum(origin));
                break;
            case FIXED_PHONE:
                jsonGenerator.writeString(SensitiveUtils.fixedPhone(origin));
                break;
            case MOBILE_PHONE:
                jsonGenerator.writeString(SensitiveUtils.mobilePhone(origin));
                break;
            case ADDRESS:
                jsonGenerator.writeString(SensitiveUtils.address(origin));
                break;
            case EMAIL:
                jsonGenerator.writeString(SensitiveUtils.email(origin));
                break;
            case BANK_CARD:
                jsonGenerator.writeString(SensitiveUtils.bankCard(origin));
                break;
            case PASSWORD:
                jsonGenerator.writeString(SensitiveUtils.password(origin));
                break;
            case KEY:
                jsonGenerator.writeString(SensitiveUtils.key(origin));
                break;
            case CUSTOMER:
                jsonGenerator
                        .writeString(SensitiveUtils.desValue(origin, prefixPlaintextLength, suffixPlaintextLength, maskCode));
                break;
            default:
                throw new IllegalArgumentException("Unknown sensitive type enum " + mode);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(final SerializerProvider serializerProvider,
                                              final BeanProperty beanProperty) throws JsonMappingException {
        if (beanProperty != null) {
            if (Objects.equals(beanProperty.getType().getRawClass(), String.class)) {
                Sensitive sensitive = beanProperty.getAnnotation(Sensitive.class);
                if (sensitive == null) {
                    sensitive = beanProperty.getContextAnnotation(Sensitive.class);
                }
                if (sensitive != null) {
                    return new SensitiveJsonSerialize(sensitive.mode(), sensitive.prefixPlaintextLength(),
                            sensitive.suffixPlaintextLength(), sensitive.maskCode());
                }
            }
            return serializerProvider.findValueSerializer(beanProperty.getType(), beanProperty);
        }
        return serializerProvider.findNullValueSerializer(null);
    }

}
