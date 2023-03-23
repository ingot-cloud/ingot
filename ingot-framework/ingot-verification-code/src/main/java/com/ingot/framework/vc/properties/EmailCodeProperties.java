package com.ingot.framework.vc.properties;

import java.util.List;

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
     * 要拦截的url, ant pattern
     */
    private List<String> urls;
    /**
     * 过期时间
     */
    private int expireIn = 60 * 60 * 24;
}
