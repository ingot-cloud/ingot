package com.ingot.framework.data.mybatis.common.ser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.ingot.framework.data.mybatis.common.model.Pagination;

/**
 * <p>Description  : PaginationJackson2Serializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/29.</p>
 * <p>Time         : 9:56 下午.</p>
 */
@SuppressWarnings("rawtypes")
public class PaginationJackson2Serializer extends StdSerializer<Pagination> {

    public PaginationJackson2Serializer() {
        super(Pagination.class);
    }

    @Override
    public void serialize(Pagination value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("size", value.getSize());
        gen.writeNumberField("current", value.getCurrent());
        gen.writeNumberField("total", value.getTotal());
        gen.writeObjectField("records", value.getRecords());
        gen.writeEndObject();
    }
}
