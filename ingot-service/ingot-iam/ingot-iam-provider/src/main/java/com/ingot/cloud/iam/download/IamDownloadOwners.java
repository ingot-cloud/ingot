package com.ingot.cloud.iam.download;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.download.DownloadOwner;

/**
 * <p>把 IAM 可信身份转换成下载任务归属。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamDownloadOwners {

    private IamDownloadOwners() {
    }

    /**
     * 取当前身份的认证域、租户与成员。
     *
     * @param actor 已恢复的可信身份
     * @return 归属
     */
    public static DownloadOwner of(ActiveIdentity actor) {
        AuthorizationContext context = actor.context();
        Long tenantId = context.tenantId() == null ? null : IamIds.require(context.tenantId());
        return new DownloadOwner(context.domain(), tenantId, IamIds.require(context.memberId()));
    }
}
