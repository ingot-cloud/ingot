package com.ingot.cloud.iam.download;

import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.download.DownloadLink;
import com.ingot.framework.commons.model.download.DownloadTask;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.download.DownloadCenter;
import com.ingot.framework.download.DownloadCenterException;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadTaskPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>租户域「我的下载」：只返回当前成员发起的任务，下载前由来源复核权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantDownloadService {
    /**
     * 任务仍在生成时的提示。
     */
    public static final String NOT_READY = "文件仍在生成，请稍后再试";
    /**
     * 对象存储暂时无法签发链接时的提示。
     */
    public static final String LINK_UNAVAILABLE = "暂时无法生成下载链接，请稍后重试";

    private final CurrentIdentityService identities;
    private final DownloadCenter downloads;

    /**
     * 分页列出当前成员的下载任务，新的在前。
     *
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     * @return 当前页
     */
    public PageResponse<DownloadTask> page(int page, int pageSize) {
        IamPages.require(page, pageSize);
        DownloadTaskPage result = downloads.page(owner(), page, pageSize);
        return IamPages.of(result.items(), result.total(), page, pageSize);
    }

    /**
     * 读取当前成员的单个任务。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    public DownloadTask get(String taskId) {
        DownloadOwner owner = owner();
        try {
            return downloads.get(owner, IamIds.require(taskId));
        } catch (DownloadCenterException exception) {
            throw translate(exception);
        }
    }

    /**
     * 签发当前成员任务的下载链接。
     *
     * @param taskId 任务 ID
     * @return 链接
     */
    public DownloadLink link(String taskId) {
        DownloadOwner owner = owner();
        try {
            return downloads.link(owner, IamIds.require(taskId));
        } catch (DownloadCenterException exception) {
            throw translate(exception);
        }
    }

    private DownloadOwner owner() {
        return IamDownloadOwners.of(identities.requireDomain(AuthorizationDomain.TENANT));
    }

    private static BizException translate(DownloadCenterException exception) {
        return switch (exception.getError()) {
            case NOT_FOUND -> new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            case NOT_READY -> new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(), NOT_READY);
            case ACCESS_DENIED -> new BizException(IamReasonCode.ACTION_DENIED);
            case LINK_UNAVAILABLE -> new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(),
                    LINK_UNAVAILABLE);
        };
    }
}
