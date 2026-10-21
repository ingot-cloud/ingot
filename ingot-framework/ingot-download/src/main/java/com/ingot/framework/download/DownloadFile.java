package com.ingot.framework.download;

/**
 * <p>导出任务生成的文件。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param filename 下载文件名
 * @param contentType MIME 类型
 * @param content 文件内容
 */
public record DownloadFile(String filename, String contentType, byte[] content) {
}
