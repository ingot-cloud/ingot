package com.ingot.framework.download;

/**
 * <p>在异步线程生成导出文件，执行时沿用发起请求的认证上下文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FunctionalInterface
public interface DownloadJob {

    /**
     * 生成文件。
     *
     * @return 非空文件
     * @throws DownloadJobException 需要向用户展示具体失败原因时
     */
    DownloadFile produce();
}
