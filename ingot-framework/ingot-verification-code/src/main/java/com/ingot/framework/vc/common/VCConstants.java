package com.ingot.framework.vc.common;

import cn.hutool.core.util.StrUtil;

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
    String BEAN_NAME_PROVIDER = "VCProvider";
    String BEAN_NAME_GENERATOR = "VCGenerator";
    String BEAN_NAME_PROVIDER_SMS = TYPE_VALUE_SMS + BEAN_NAME_PROVIDER;
    String BEAN_NAME_GENERATOR_SMS = TYPE_VALUE_SMS + BEAN_NAME_GENERATOR;
    String BEAN_NAME_PROVIDER_EMAIL = TYPE_VALUE_EMAIL + BEAN_NAME_PROVIDER;
    String BEAN_NAME_GENERATOR_EMAIL = TYPE_VALUE_EMAIL + BEAN_NAME_GENERATOR;
    String BEAN_NAME_PROVIDER_IMAGE = TYPE_VALUE_IMAGE + BEAN_NAME_GENERATOR;
    String BEAN_NAME_GENERATOR_IMAGE = TYPE_VALUE_IMAGE + BEAN_NAME_PROVIDER;
}
