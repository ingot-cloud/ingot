package com.ingot.cloud.pms;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>Description  : Test.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/10.</p>
 * <p>Time         : 08:39.</p>
 */
public class UnitTest {

    @Test
    public void testUuid() {
//        System.out.println(StrUtil.uuid());
//        System.out.println(StrUtil.uuid());
//        System.out.println(StrUtil.uuid().replaceAll("-", ""));
    }

    @Test
    public void base64() {
        String raw = "ingotingotingotingotingotingotingot";
        System.out.println(Base64.encode(raw));
    }

    @Test
    public void testPassword() {
        String password = "111111";
        PasswordEncoder encode = new BCryptPasswordEncoder();
        System.out.println(encode.encode(password));
//        15046238819
    }

    @Test
    public void testJson() {
        ObjectMapper objectMapper = new ObjectMapper();
//        TestObj obj = new TestObj();
//        obj.setTest("测试,测试2");
//
//        String strValue = null;
//        try {
//            strValue = objectMapper.writeValueAsString(obj);
//            System.out.println("write: " + strValue);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }

        String strValue = "{\"test\":\"测试,测试2\"}";

        TestObj read = null;
        try {
            read = objectMapper.readValue(strValue, TestObj.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        System.out.println("read: " + read);

    }

    @Data
    public static class TestObj {
        private String test;
    }
}
