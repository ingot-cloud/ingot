package com.ingot.framework.security.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.exception.IngotOAuth2Exception;
import lombok.SneakyThrows;

/**
 * <p>Description  : IngotOAuth2ExceptionSerializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:29.</p>
 */
public class IngotOAuth2ExceptionSerializer extends StdSerializer<IngotOAuth2Exception> {

    public IngotOAuth2ExceptionSerializer() {
        super(IngotOAuth2Exception.class);
    }

    @SneakyThrows
    @Override public void serialize(IngotOAuth2Exception value,
                                    JsonGenerator gen,
                                    SerializerProvider provider) {
        gen.writeStartObject();
        gen.writeStringField(R.CODE, value.getOAuth2ErrorCode());
        gen.writeStringField(R.MESSAGE, value.getMessage());
        if (StrUtil.isNotEmpty(value.getRaw())){
            gen.writeObjectField(R.DATA, value.getRaw());
        }
        gen.writeEndObject();
    }
}
