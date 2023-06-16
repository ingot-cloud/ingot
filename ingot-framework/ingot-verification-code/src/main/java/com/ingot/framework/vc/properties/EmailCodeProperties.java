package com.ingot.framework.vc.properties;

import lombok.Data;

/**
 * <p>Description  : EmailCodeProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/23.</p>
 * <p>Time         : 7:41 PM.</p>
 */
@Data
public class EmailCodeProperties {
    /**
     * 验证码长度
     */
    private int length = 6;
    /**
     * 过期时间, 单位秒
     */
    private int expireIn = 10 * 60;
    /**
     * 操作频率，单位秒
     */
    private int opsRate = 60;
}
