package com.ingot.framework.security.access.service;

import java.time.Duration;

/**
 * 登录失败滑动窗口计数端口。
 *
 * @author jy
 * @since 1.0.0
 */
public interface LoginFailureCounter {

    long increment(String counterKey, Duration window);

    void reset(String counterKey);
}
