package com.ingot.cloud.iam.download;

import java.util.List;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.download.DownloadCenter;
import com.ingot.framework.download.DownloadCenterError;
import com.ingot.framework.download.DownloadCenterException;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadTaskPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证「我的下载」按当前租户成员取归属，并把下载中心拒绝映射成 IAM 错误码。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantDownloadServiceTest {
    private static final AuthorizationContext CONTEXT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final DownloadOwner OWNER = new DownloadOwner(AuthorizationDomain.TENANT, 10L, 101L);

    private final CurrentIdentityService identities = mock(CurrentIdentityService.class);
    private final DownloadCenter center = mock(DownloadCenter.class);
    private final TenantDownloadService service = new TenantDownloadService(identities, center);

    @Test
    void pageUsesCurrentTenantMember() {
        when(identities.requireDomain(AuthorizationDomain.TENANT))
                .thenReturn(new ActiveIdentity(CONTEXT, "0", "0", "0"));
        when(center.page(OWNER, 2, 10)).thenReturn(new DownloadTaskPage(List.of(), 11));

        var page = service.page(2, 10);

        assertEquals(11, page.total());
        verify(center).page(OWNER, 2, 10);
    }

    @Test
    void centerErrorsMapToIamCodes() {
        when(identities.requireDomain(AuthorizationDomain.TENANT))
                .thenReturn(new ActiveIdentity(CONTEXT, "0", "0", "0"));
        assertCode(DownloadCenterError.NOT_FOUND, IamReasonCode.OBJECT_NOT_FOUND.getCode());
        assertCode(DownloadCenterError.ACCESS_DENIED, IamReasonCode.ACTION_DENIED.getCode());
        assertCode(DownloadCenterError.NOT_READY, IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode());
        assertCode(DownloadCenterError.LINK_UNAVAILABLE, IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode());
    }

    private void assertCode(DownloadCenterError error, String code) {
        doThrow(new DownloadCenterException(error)).when(center).link(OWNER, 5L);
        BizException exception = assertThrows(BizException.class, () -> service.link("5"));
        assertEquals(code, exception.getCode());
    }
}
