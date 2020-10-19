package com.ingot.common.base.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>Description  : NonTimeZoneDateSerialize.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/14.</p>
 * <p>Time         : 5:32 PM.</p>
 */
@Slf4j
public class NonTimeZoneDateSerialize extends StdSerializer<Date> {

    public NonTimeZoneDateSerialize() {
        super(Date.class);
    }

    @Override public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        gen.writeString(format.format(value));
    }
}
