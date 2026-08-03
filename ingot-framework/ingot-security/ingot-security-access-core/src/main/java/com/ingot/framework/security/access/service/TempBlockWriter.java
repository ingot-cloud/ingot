package com.ingot.framework.security.access.service;

import java.time.Duration;

/**
 * 临时封禁写入端口。
 *
 * @author jy
 * @since 1.0.0
 */
public interface TempBlockWriter {

    void block(String keyType, String keyValue, Duration ttl);
}
