package com.ingot.framework.security.core.encoder;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>Description  : NoPasswordEncoder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/19.</p>
 * <p>Time         : 11:14 AM.</p>
 */
public class NoPasswordEncoder implements PasswordEncoder {
    @Override public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override public boolean matches(CharSequence charSequence, String s) {
        return this.encode(charSequence.toString()).equals(s);
    }
}
