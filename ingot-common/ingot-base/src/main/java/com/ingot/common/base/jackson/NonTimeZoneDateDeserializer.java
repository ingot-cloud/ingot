package com.ingot.common.base.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.util.Date;

/**
 * <p>Description  : NonTimeZoneDateDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/15.</p>
 * <p>Time         : 12:02 PM.</p>
 */
@Slf4j
public class NonTimeZoneDateDeserializer extends StdDeserializer<Date> {

    public NonTimeZoneDateDeserializer() {
        super(Date.class);
    }

    @Override public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String date = p.readValueAs(String.class);
        return DateTime.parse(date, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")).toDate();
    }
}
