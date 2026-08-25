package com.ingot.framework.security.account.adapter.policy;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.AccountLockoutPolicyVO;
import com.ingot.cloud.security.api.rpc.RemoteAccountLockoutPolicyService;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RemoteAccountLockoutPolicyLoader} 远端成功 / 失败 / 空列表语义。
 *
 * @author jy
 * @since 1.0.0
 */
class RemoteAccountLockoutPolicyLoaderTest {

    private final RemoteAccountLockoutPolicyService remoteService = mock(RemoteAccountLockoutPolicyService.class);
    private final RemoteAccountLockoutPolicyLoader loader = new RemoteAccountLockoutPolicyLoader(remoteService);

    @Test
    void load_success_mapsPolicies() {
        AccountLockoutPolicyVO vo = new AccountLockoutPolicyVO();
        vo.setUserType(UserTypeEnum.ADMIN);
        vo.setEnabled(true);
        vo.setMaxAttempts(5);
        vo.setLockDurationMinutes(30);
        vo.setAttemptWindowMinutes(15);
        vo.setHintAfterAttempts(3);
        when(remoteService.listPolicies()).thenReturn(R.ok(List.of(vo)));

        List<LockoutPolicy> result = loader.load("all");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userType()).isEqualTo(UserTypeEnum.ADMIN);
        assertThat(result.get(0).maxAttempts()).isEqualTo(5);
    }

    @Test
    void load_emptyList_throwsUnavailable() {
        when(remoteService.listPolicies()).thenReturn(R.ok(List.of()));

        assertThatThrownBy(() -> loader.load("all"))
                .isInstanceOf(AccountLockoutPolicyRemoteUnavailableException.class);
    }

    @Test
    void load_feignError_throwsUnavailable() {
        when(remoteService.listPolicies()).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> loader.load("all"))
                .isInstanceOf(AccountLockoutPolicyRemoteUnavailableException.class);
    }
}
