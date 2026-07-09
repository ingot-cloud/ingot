package com.ingot.framework.security.crypto.annotation;

import java.lang.annotation.*;

import com.ingot.framework.security.crypto.model.CryptoType;

/**
 * <p>信封加密（HYBRID）上下文标记注解，是拦截器建立加解密上下文的唯一触发点。</p>
 *
 * <p>标记该注解的端点，会在 {@code HybridCryptoInterceptor.preHandle} 阶段完成协议头解析、
 * 防重放校验、RSA-OAEP 解包 CEK，并将 CEK/AAD 写入 request attribute、回带响应头；
 * 它<b>不</b>负责真实加解密。整体模式配合 {@link InDecrypt}({@link CryptoType#HYBRID})/
 * {@link InEncrypt}({@link CryptoType#HYBRID})；字段级模式配合
 * {@link InDecryptField}/{@link InEncryptField}，加解密分别由请求/响应 Advice 与字段级序列化器完成。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 仅在处理器方法或控制器类上标注生效，标注在 DTO/返回实体类上不会被识别。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface InCryptoHybridContext {
}
