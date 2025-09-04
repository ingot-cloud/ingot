package com.ingot.framework.oss.jackson;

import java.io.IOException;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.ingot.framework.oss.common.OssObjectInfo;
import com.ingot.framework.oss.common.OssPathParser;

/**
 * <p>Description  : OssSaveUrlDeserializer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/9/4.</p>
 * <p>Time         : 17:29.</p>
 */
public class OssSaveUrlDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if (p == null || StrUtil.isEmpty(p.getText())) {
            return null;
        }

        OssObjectInfo object = OssPathParser.parse(p.getText());
        return object.bucket() + "/" + object.objectName();
    }
}
