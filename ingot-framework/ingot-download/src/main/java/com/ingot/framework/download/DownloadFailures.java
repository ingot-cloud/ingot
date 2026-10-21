package com.ingot.framework.download;

/**
 * <p>框架写入任务的可展示失败原因。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DownloadFailures {
    /**
     * 生成文件时出现未预期异常。
     */
    public static final String GENERATE = "文件生成失败，请重新导出";
    /**
     * 上传对象存储或记录结果失败。
     */
    public static final String STORE = "文件保存失败，请重新导出";
    /**
     * 执行队列已满。
     */
    public static final String BUSY = "导出任务较多，请稍后重新导出";
    /**
     * 超过执行时限仍未完成，通常是节点重启。
     */
    public static final String STALE = "任务执行超时，请重新导出";

    private DownloadFailures() {
    }
}
