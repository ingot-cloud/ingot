package com.ingot.framework.download;

/**
 * <p>下载中心对调用方的拒绝类别，由宿主映射成自身错误码。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum DownloadCenterError {
    /** 任务不存在、不属于当前身份、失败或已过期。 */
    NOT_FOUND,
    /** 任务仍在排队或生成。 */
    NOT_READY,
    /** 来源未注册权限复核，按拒绝处理。 */
    ACCESS_DENIED,
    /** 对象存储暂时无法签发链接。 */
    LINK_UNAVAILABLE
}
