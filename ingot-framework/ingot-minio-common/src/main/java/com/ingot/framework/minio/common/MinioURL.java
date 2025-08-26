package com.ingot.framework.minio.common;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.minio.jackson.MinioURLSerializer;

/**
 * <p>Description  : MinioURL.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 11:25.</p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = MinioURLSerializer.class)
public @interface MinioURL {
    /**
     * 默认(-1)使用配置文件中的过期时间，单位秒
     *
     * @return 过期时间
     */
    int expireSeconds() default -1;
}
