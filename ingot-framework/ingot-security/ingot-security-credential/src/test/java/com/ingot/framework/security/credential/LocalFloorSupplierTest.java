package com.ingot.framework.security.credential;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.internal.LocalFloorSupplier;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LocalFloorSupplier} 属性→VO 映射与安全基线非空测试。
 *
 * @author jy
 * @since 1.0.0
 */
class LocalFloorSupplierTest {

    @Test
    void defaults_includeCheckableAndInitialPassword() {
        List<CredentialPolicyConfigVO> floor = new LocalFloorSupplier(new CredentialSecurityProperties()).get();

        assertFalse(floor.isEmpty());
        assertTrue(floor.stream().anyMatch(vo -> CredentialPolicyType.STRENGTH.getValue().equals(vo.getPolicyType())),
                "默认应含强度策略");
        assertTrue(floor.stream().anyMatch(vo -> CredentialPolicyType.INITIAL_PASSWORD.getValue().equals(vo.getPolicyType())),
                "应始终含初始密码策略");
    }

    @Test
    void allCheckableDisabled_stillHasStrengthBaseline() {
        CredentialSecurityProperties properties = new CredentialSecurityProperties();
        properties.getPolicy().getStrength().setEnabled(false);
        properties.getPolicy().getHistory().setEnabled(false);
        properties.getPolicy().getExpiration().setEnabled(false);

        List<CredentialPolicyConfigVO> floor = new LocalFloorSupplier(properties).get();

        boolean hasCheckable = floor.stream()
                .anyMatch(vo -> !CredentialPolicyType.INITIAL_PASSWORD.getValue().equals(vo.getPolicyType()));
        assertTrue(hasCheckable, "全部关闭时应补最小强度基线，避免 fail-open");
    }
}
