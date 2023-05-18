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

    String URL_PREFIX = "/code";

    /**
     * 请求参数，接收人，比如手机号，邮箱等
     */
    String QUERY_PARAMS_RECEIVER = "_vc_receiver";
    /**
     * 请求参数，验证码
     */
    String QUERY_PARAMS_CODE = "_vc_code";
}
