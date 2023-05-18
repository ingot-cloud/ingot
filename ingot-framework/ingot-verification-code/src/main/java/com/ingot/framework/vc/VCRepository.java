package com.ingot.framework.vc;

import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.common.VC;

/**
 * <p>Description  : VCRepository.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/27.</p>
 * <p>Time         : 9:37 AM.</p>
 */
public interface VCRepository {

    /**
     * 获取验证码
     *
     * @param key  验证码KEY
     * @param type 验证码类型 {@link VCType}
     * @return {@link VC}
     */
    VC get(String key, VCType type);

    /**
     * 保存验证码
     *
     * @param key  验证码KEY
     * @param code 验证码 {@link VC}
     */
    void save(String key, VC code);

    /**
     * 清空验证码
     *
     * @param key  验证码KEY
     * @param type 验证码类型 {@link VCType}
     */
    void clear(String key, VCType type);
}
