package com.ingot.cloud.iam.download;

import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadSourceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>下载成员表格导出文件前，要求当前身份仍具备成员导出权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberWorkbookDownloadAccess implements DownloadSourceAccess {
    private final IamAccess access;

    @Override
    public String source() {
        return IamDownloadSources.MEMBER_WORKBOOK;
    }

    @Override
    public void check(DownloadOwner owner) {
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
    }
}
