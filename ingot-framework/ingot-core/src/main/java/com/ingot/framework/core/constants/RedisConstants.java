package com.ingot.framework.core.constants;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Preconditions;
import com.ingot.framework.core.utils.DigestUtils;

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
    String BASE_PREFIX = "ingot:";

    /**
     * Redis JWT key
     */
    String REDIS_JWT_PRI_KEY = BASE_PREFIX + "security:jwt:pri";
    String REDIS_JWT_PUB_KEY = BASE_PREFIX + "security:jwt:pub";

    String REDIS_USER_ACCESS_TOKEN_KEY_PREFIX = BASE_PREFIX + "security:user:ACCESS_TOKEN:";

    String REDIS_VALIDATE_CODE_PRE = BASE_PREFIX + "validate_code:";
    String REDIS_SEND_SMS_COUNT = REDIS_VALIDATE_CODE_PRE + "sms:count";

    /**
     * Redis 用户 token 前缀
     */
    static String userTokenPreKey(String userId, String username){
        Preconditions.checkArgument(StrUtil.isNotEmpty(userId), "非法请求token参数不存在");
        Preconditions.checkArgument(StrUtil.isNotEmpty(username), "非法请求token参数不存在");
        final String key = userId + username;
        return REDIS_USER_ACCESS_TOKEN_KEY_PREFIX + DigestUtils.sha256(key);
    }

    /**
     * Gets send sms count key.
     *
     * @param ipAddr the ip addr
     * @param type   mobile;ip;total
     *
     * @return the send sms count key
     */
    static String getSendSmsCountKey(String ipAddr, String type) {
        Preconditions.checkArgument(StrUtil.isNotEmpty(ipAddr), "请不要篡改IP地址");
        return REDIS_SEND_SMS_COUNT + ":" + type + ":" + ipAddr;
    }

    /**
     * Gets send sms rate key.
     *
     * @param ipAddr the ip addr
     *
     * @return the send sms rate key
     */
    static String getSendSmsRateKey(String ipAddr) {
        Preconditions.checkArgument(StrUtil.isNotEmpty(ipAddr), "请不要篡改IP地址");
        return REDIS_SEND_SMS_COUNT + ":" + ipAddr;
    }
}
