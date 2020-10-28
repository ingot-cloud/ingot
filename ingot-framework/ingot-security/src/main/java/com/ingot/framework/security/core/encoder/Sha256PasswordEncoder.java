package com.ingot.framework.security.core.encoder;

import org.apache.commons.codec.binary.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * <p>Description  : Sha256PasswordEncoder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/11.</p>
 * <p>Time         : 下午12:57.</p>
 */
public class Sha256PasswordEncoder implements PasswordEncoder {

    @Override public String encode(CharSequence charSequence) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(charSequence.toString().getBytes(StandardCharsets.UTF_8));
            return new String(Base64.encodeBase64(digest));
        } catch (Exception e) {
            return null;
        }
    }

    @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return this.encode(rawPassword.toString()).equals(encodedPassword);
    }
}
