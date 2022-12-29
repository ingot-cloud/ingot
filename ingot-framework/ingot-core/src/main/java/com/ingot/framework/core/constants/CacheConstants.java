package com.ingot.framework.core.constants;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.utils.AssertionUtils;

/**
 * <p>Description  : 缓存常量.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/4.</p>
 * <p>Time         : 下午2:09.</p>
 */
public interface CacheConstants {

    /**
     * 忽略tenant前缀
     */
    String IGNORE_TENANT_PREFIX = "in";

    /**
     * OAuth2 客户端详情
     */
    String CLIENT_DETAILS = "client_details";

    /**
     * 所有权限
     */
    String AUTHORITY_DETAILS= "authority_details";

    /**
     * 菜单
     */
    String MENU_DETAILS = "menu_details";

    /**
     * 授权信息
     */
    String AUTHORIZATION_DETAILS = "auth_details";

    interface Security {
        String PREFIX = IGNORE_TENANT_PREFIX + ":security";
        /**
         * 授权私钥
         */
        String AUTHORIZATION_PRI = PREFIX + ":key_pri";
        /**
         * 公钥
         */
        String AUTHORIZATION_PUB = PREFIX + ":key_pub";
        /**
         * Key ID
         */
        String AUTHORIZATION_KEY_ID = PREFIX + ":key_id";
    }

    interface Validator {
        String CODE_PRE = "validate_code";
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
