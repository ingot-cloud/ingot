package com.ingot.framework.download;

/**
 * <p>按来源复核下载权限。每个接入的导出来源注册一个实现，未注册的来源不能登记也不能下载。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface DownloadSourceAccess {

    /**
     * 返回负责的来源编码。
     *
     * @return 来源编码
     */
    String source();

    /**
     * 在请求线程确认当前身份仍可下载该来源的文件。
     *
     * @param owner 任务归属，已与当前身份一致
     * @throws RuntimeException 无权限时抛出业务异常，原样传给调用方
     */
    void check(DownloadOwner owner);
}
