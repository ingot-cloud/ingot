package com.ingot.framework.security.credential;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.credential.internal.CredentialRemoteUnavailableException;
import com.ingot.framework.security.credential.internal.RemoteCredentialPolicyConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RemoteCredentialPolicyConfigService} 区分「失败」与「合法空」的单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class RemoteCredentialPolicyConfigServiceTest {

    private final RemoteCredentialService remote = mock(RemoteCredentialService.class);
    private final RemoteCredentialPolicyConfigService service = new RemoteCredentialPolicyConfigService(remote);

    @Test
    void success_returnsData() {
        CredentialPolicyConfigVO vo = new CredentialPolicyConfigVO();
        vo.setPolicyType("1");
        when(remote.getPolicyConfigs()).thenReturn(R.ok(List.of(vo)));

        assertTrue(service.getAll().size() == 1);
    }

    @Test
    void successEmpty_returnsEmptyNoThrow() {
        when(remote.getPolicyConfigs()).thenReturn(R.ok(List.of()));

        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void nonSuccessCode_throwsUnavailable() {
        when(remote.getPolicyConfigs()).thenReturn(R.error500());

        assertThrows(CredentialRemoteUnavailableException.class, service::getAll);
    }

    @Test
    void nullResponse_throwsUnavailable() {
        when(remote.getPolicyConfigs()).thenReturn(null);

        assertThrows(CredentialRemoteUnavailableException.class, service::getAll);
    }

    @Test
    void remoteThrows_throwsUnavailable() {
        when(remote.getPolicyConfigs()).thenThrow(new RuntimeException("conn refused"));

        assertThrows(CredentialRemoteUnavailableException.class, service::getAll);
    }
}
