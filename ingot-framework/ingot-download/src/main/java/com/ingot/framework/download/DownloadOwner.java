package com.ingot.framework.download;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;

/**
 * <p>下载任务的归属身份，列表、详情与链接都按三者同时过滤。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param domain 认证域
 * @param tenantId 租户 ID；平台域为空
 * @param memberId 当前域成员 ID
 */
public record DownloadOwner(AuthorizationDomain domain, Long tenantId, long memberId) {

    /**
     * 校验租户与域匹配。
     *
     * @throws IllegalArgumentException 域缺失，或租户域缺少租户 ID
     */
    public DownloadOwner {
        if (domain == null) {
            throw new IllegalArgumentException("下载任务归属域必填");
        }
        if (domain == AuthorizationDomain.TENANT && tenantId == null) {
            throw new IllegalArgumentException("租户域下载任务必须绑定租户");
        }
    }
}
