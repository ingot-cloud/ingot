package com.ingot.framework.oss.common;

import java.time.Duration;

/**
 * <p>S3 兼容 OSS 客户端的共享默认值，避免各实现复制 region 与超时字面量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class OssDefaults {

    /**
     * 默认签名区域，自建 MinIO / RustFS 常用值。
     */
    public static final String DEFAULT_REGION = "us-east-1";

    /**
     * 默认 TCP 连接超时，用于 OSS 未启动或网络黑洞时尽快失败。
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /**
     * 默认读写超时，对齐 MinIO Java SDK 缺省 5 分钟，避免大文件传输被缩短。
     */
    public static final Duration DEFAULT_TRANSFER_TIMEOUT = Duration.ofMinutes(5);

    private OssDefaults() {
    }
}
