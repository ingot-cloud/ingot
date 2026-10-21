package com.ingot.framework.download;

/**
 * <p>导出生成失败且原因可以展示给发起人，原因会写入任务的失败原因。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class DownloadJobException extends RuntimeException {

    /**
     * 创建带展示原因的失败。
     *
     * @param reason 可展示原因，不含敏感信息
     */
    public DownloadJobException(String reason) {
        super(reason);
    }
}
