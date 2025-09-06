package com.ingot.framework.commons.jackson;

import java.io.IOException;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * <p>Description  : IPageSerializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/26.</p>
 * <p>Time         : 11:47 上午.</p>
 */
public class IPageSerializer extends StdSerializer<IPage> {

    protected IPageSerializer() {
        super(IPage.class);
    }

    @Override
    public void serialize(IPage value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("current", value.getCurrent());
        gen.writeNumberField("size", value.getSize());
        gen.writeNumberField("total", value.getTotal());
        gen.writeObjectField("records", value.getRecords());
        gen.writeEndObject();
    }
}
