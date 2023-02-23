package com.ingot.component.id;

/**
 * <p>Description  : BizGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/2/23.</p>
 * <p>Time         : 3:15 PM.</p>
 */
public interface BizGenerator {

    /**
     * 业务类型
     *
     * @param key 业务key
     * @return ID
     */
    long getId(String key);
}
