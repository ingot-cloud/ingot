package com.ingot.cloud.iam.application.download;

import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadSourceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>下载部门表格导出文件前，要求当前身份仍具备部门导出权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DepartmentWorkbookDownloadAccess implements DownloadSourceAccess {
    private final IamAccess access;

    @Override
    public String source() {
        return IamDownloadSources.DEPARTMENT_WORKBOOK;
    }

    @Override
    public void check(DownloadOwner owner) {
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_EXPORT);
    }
}
