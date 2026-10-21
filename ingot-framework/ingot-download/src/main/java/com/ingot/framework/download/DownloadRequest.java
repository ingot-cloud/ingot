package com.ingot.framework.download;

/**
 * <p>登记下载任务时的归属与展示信息。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param owner 发起人
 * @param source 来源编码，须有对应 {@link DownloadSourceAccess}
 * @param title 列表展示名称
 */
public record DownloadRequest(DownloadOwner owner, String source, String title) {
}
