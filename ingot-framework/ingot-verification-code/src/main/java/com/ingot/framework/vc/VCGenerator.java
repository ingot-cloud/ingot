package com.ingot.framework.vc;

import com.ingot.framework.vc.common.VC;

/**
 * <p>Description  : VCGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/28.</p>
 * <p>Time         : 10:56 AM.</p>
 */
public interface VCGenerator {

    /**
     * 生成校验码
     *
     * @return {@link VC}
     */
    VC generate();
}
