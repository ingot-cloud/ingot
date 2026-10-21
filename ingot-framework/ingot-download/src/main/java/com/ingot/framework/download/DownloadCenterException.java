package com.ingot.framework.download;

import lombok.Getter;

/**
 * <p>下载中心拒绝请求，携带拒绝类别。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
public class DownloadCenterException extends RuntimeException {
    /**
     * 拒绝类别。
     */
    private final DownloadCenterError error;

    /**
     * 创建拒绝。
     *
     * @param error 拒绝类别
     */
    public DownloadCenterException(DownloadCenterError error) {
        super(error.name());
        this.error = error;
    }
}
