package com.ingot.framework.vc.common;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.utils.DigestUtil;

/**
 * <p>Description  : VCConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/27.</p>
 * <p>Time         : 10:52 AM.</p>
 */
public interface VCConstants {

    /**
     * 验证码缓存前缀
     */
    String CACHE_KEY_PREFIX = "vc_details";

    /**
     * 保存验证码
     */
    String CACHE_CODE = CACHE_KEY_PREFIX + ":code";

    /**
     * 保存短信检查参数前缀
     */
    String CACHE_SMS_CHECK = CACHE_KEY_PREFIX + ":sms";

    /**
     * 保存邮件检查参数前缀
     */
    String CACHE_EMAIL_CHECK = CACHE_KEY_PREFIX + ":email";

    /**
     * 图形验证码检查参数前缀
     */
    String CACHE_CAPTCHA_CHECK = CACHE_KEY_PREFIX + ":captcha";

    /**
     * 获取仓库KEY
     *
     * @param key  自定义key
     * @param type {@link VCType} 类型
     * @return 仓库KEY
     */
    static String getRepositoryKey(String key, VCType type) {
        InnerCheck.check(StrUtil.isNotEmpty(key) && type != null,
                "vc.common.repositoryKey");
        return CACHE_CODE + ":" + type.getValue() + ":" + key;
    }

    /**
     * 获取短信检查Key
     *
     * @param receiver 接收者
     * @param remoteIP IP
     * @param type     类型
     * @return cache key
     */
    static String getSmsCheckKey(String receiver, String remoteIP, String type) {
        String digest = DigestUtil.md5(receiver + remoteIP);
        return CACHE_SMS_CHECK + ":" + type + ":" + digest;
    }

    /**
     * 获取邮件检查Key
     *
     * @param receiver 接收者
     * @param remoteIP IP
     * @return cache key
     */
    static String getEmailCheckKey(String receiver, String remoteIP) {
        String digest = DigestUtil.md5(receiver + remoteIP);
        return CACHE_EMAIL_CHECK + ":" + digest;
    }

    /**
     * 获取图形检查Key
     *
     * @param receiver 接收者
     * @param remoteIP IP
     * @return cache key
     */
    static String getCaptchaCheckKey(String receiver, String remoteIP) {
        String digest = DigestUtil.md5(receiver + remoteIP);
        return CACHE_CAPTCHA_CHECK + ":" + digest;
    }

    /**
     * api前缀
     */
    String PATH_PREFIX = "/vc";

    /**
     * 请求参数，接收人，比如手机号，邮箱等
     */
    String QUERY_PARAMS_RECEIVER = "_vc_receiver";
    /**
     * 请求参数，验证码
     */
    String QUERY_PARAMS_CODE = "_vc_code";

    /**
     * 验证码类型
     */
    String TYPE_VALUE_SMS = "sms";
    String TYPE_VALUE_EMAIL = "email";
    String TYPE_VALUE_IMAGE = "image";
    String BEAN_NAME_PROCESSOR = "VCProcessor";
    String BEAN_NAME_PROVIDER = "VCProvider";
    String BEAN_NAME_GENERATOR = "VCGenerator";
    String BEAN_NAME_SEND_CHECKER = "VCSendChecker";
    // SMS
    String BEAN_NAME_PROVIDER_SMS = TYPE_VALUE_SMS + BEAN_NAME_PROVIDER;
    String BEAN_NAME_PROCESSOR_SMS = TYPE_VALUE_SMS + BEAN_NAME_PROCESSOR;
    String BEAN_NAME_GENERATOR_SMS = TYPE_VALUE_SMS + BEAN_NAME_GENERATOR;
    String BEAN_NAME_SEND_CHECKER_SMS = TYPE_VALUE_SMS + BEAN_NAME_SEND_CHECKER;
    // EMAIL
    String BEAN_NAME_PROVIDER_EMAIL = TYPE_VALUE_EMAIL + BEAN_NAME_PROVIDER;
    String BEAN_NAME_PROCESSOR_EMAIL = TYPE_VALUE_EMAIL + BEAN_NAME_PROCESSOR;
    String BEAN_NAME_GENERATOR_EMAIL = TYPE_VALUE_EMAIL + BEAN_NAME_GENERATOR;
    String BEAN_NAME_SEND_CHECKER_EMAIL = TYPE_VALUE_EMAIL + BEAN_NAME_SEND_CHECKER;
    // IMAGE
    String BEAN_NAME_PROVIDER_IMAGE = TYPE_VALUE_IMAGE + BEAN_NAME_PROVIDER;
    String BEAN_NAME_PROCESSOR_IMAGE = TYPE_VALUE_IMAGE + BEAN_NAME_PROCESSOR;
    String BEAN_NAME_GENERATOR_IMAGE = TYPE_VALUE_IMAGE + BEAN_NAME_GENERATOR;
    String BEAN_NAME_SEND_CHECKER_IMAGE = TYPE_VALUE_IMAGE + BEAN_NAME_SEND_CHECKER;

    /**
     * 滑块验证码
     */
    String IMAGE_CODE_TYPE = "blockPuzzle";
}
