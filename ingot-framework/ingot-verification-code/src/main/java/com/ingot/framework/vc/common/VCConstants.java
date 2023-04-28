package com.ingot.framework.vc.common;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.vc.VCGenerator;

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
        AssertionUtils.check(StrUtil.isNotEmpty(key) && type != null, () -> {
            throw new VCException("仓库Key参数异常");
        });
        return CACHE_CODE + ":" + type.getValue() + ":" + key;
    }

    /**
     * 获取生成验证码类 Bean Name
     *
     * @param type {@link VCType}
     * @return bean name
     */
    static String getGeneratorBeanName(VCType type) {
        return type.getBeanNamePrefix() + VCGenerator.class.getSimpleName();
    }
}
