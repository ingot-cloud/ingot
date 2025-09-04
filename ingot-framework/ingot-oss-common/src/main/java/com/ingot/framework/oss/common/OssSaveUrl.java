package com.ingot.framework.oss.common;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ingot.framework.oss.jackson.OssSaveUrlDeserializer;

/**
 * <p>Description  : OssSaveUrl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/9/4.</p>
 * <p>Time         : 17:26.</p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = OssSaveUrlDeserializer.class)
public @interface OssSaveUrl {
}
