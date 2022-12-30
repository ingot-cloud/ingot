package com.ingot.framework.core.utils;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.core.codec.Base64;

/**
 * <p>Description  : AESUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/30.</p>
 * <p>Time         : 3:10 PM.</p>
 */
public final class AESUtils {

    public static String decryptAES(String key, String data) {
        AES aes = new AES(Mode.CBC, Padding.NoPadding,
                new SecretKeySpec(key.getBytes(), "AES"),
                new IvParameterSpec(key.getBytes()));
        byte[] result = aes.decrypt(Base64.decode(data.getBytes(StandardCharsets.UTF_8)));
        return new String(result, StandardCharsets.UTF_8).trim();
    }

}
