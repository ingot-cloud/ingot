package com.ingot.framework.oss.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.ingot.framework.oss.common.OssObjectInfo;
import com.ingot.framework.oss.common.OssPathParser;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : OssSaveUrlDeserializer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/9/4.</p>
 * <p>Time         : 17:29.</p>
 */
@Slf4j
public class OssSaveUrlDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if (p == null) {
            return null;
        }
        JsonNode node = p.getCodec().readTree(p);

        if (node.isArray()) {
            List<String> result = new ArrayList<>();

            // 处理数组 ["a", "b", "c"]
            for (JsonNode element : node) {
                String value = element.asText().trim();
                if (!value.isEmpty()) {
                    result.add(parseValue(value));
                }
            }

            return result;
        }

        if (node.isTextual()) {
            // 处理字符串
            String text = node.asText().trim();

            if (text.isEmpty()) {
                return null;
            }
            return parseValue(text);
        }

        log.info("OssSaveUrlDeserializer node数据非法={}", node);
        return null;
    }

    private String parseValue(String path) {
        OssObjectInfo object = OssPathParser.parse(path);
        return object.bucket() + "/" + object.objectName();
    }
}
