package com.ingot.framework.core.constants;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.utils.AssertionUtils;

/**
 * <p>Description  : RedisConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/4.</p>
 * <p>Time         : 下午2:09.</p>
 */
public interface RedisConstants {

    /**
     * Redis 前缀
     */
    String BASE_PREFIX = "ingot";

    interface Cache {
        /**
         * cache 前缀
         */
        String CACHE_PREFIX = BASE_PREFIX + ":cache";

        /**
         * OAuth2 客户端缓存 key
         */
        String REGISTERED_CLIENT_KEY = CACHE_PREFIX + ":client";
    }

    interface Security {
        String PREFIX = BASE_PREFIX + ":security";

        /**
         * OAuth2 授权信息 key
         */
        String AUTHORIZATION = PREFIX + ":auth";
        /**
         * 授权私钥
         */
        String AUTHORIZATION_PRI = PREFIX + ":key:pri";
        /**
         * 公钥
         */
        String AUTHORIZATION_PUB = PREFIX + ":key:pub";
        /**
         * Key ID
         */
        String AUTHORIZATION_KEY_ID = PREFIX + ":key:id";
    }

    interface Validator {
        String CODE_PRE = BASE_PREFIX + ":validate_code";
        String SEND_SMS_COUNT = CODE_PRE + ":sms:count";

        /**
         * Gets send sms count key.
         *
         * @param ipAddr the ip addr
         * @param type   mobile;ip;total
         * @return the send sms count key
         */
        static String getSendSmsCountKey(String ipAddr, String type) {
            AssertionUtils.checkArgument(StrUtil.isNotEmpty(ipAddr), "请不要篡改IP地址");
            return SEND_SMS_COUNT + ":" + type + ":" + ipAddr;
        }

        /**
         * Gets send sms rate key.
         *
         * @param ipAddr the ip addr
         * @return the send sms rate key
         */
        static String getSendSmsRateKey(String ipAddr) {
            AssertionUtils.checkArgument(StrUtil.isNotEmpty(ipAddr), "请不要篡改IP地址");
            return SEND_SMS_COUNT + ":" + ipAddr;
        }

    }
}
