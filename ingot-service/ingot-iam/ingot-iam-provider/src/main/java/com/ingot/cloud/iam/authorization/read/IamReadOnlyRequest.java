package com.ingot.cloud.iam.authorization.read;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>声明同步只读 MVC 方法可以在本请求内复用身份和两种授权来源。</p>
 * @author jy
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IamReadOnlyRequest {
}
