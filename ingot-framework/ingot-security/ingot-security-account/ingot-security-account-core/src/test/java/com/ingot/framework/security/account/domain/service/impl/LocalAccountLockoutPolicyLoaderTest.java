package com.ingot.framework.security.account.domain.service.impl;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.config.AccountDomainProperties;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LocalAccountLockoutPolicyLoader} 即时映射 Nacos 属性。
 *
 * @author jy
 * @since 1.0.0
 */
class LocalAccountLockoutPolicyLoaderTest {

    @Test
    void mapsPropertiesAndCarriesUserType() {
        AccountDomainProperties properties = new AccountDomainProperties();
        properties.getLockout().setMaxAttempts(3);
        properties.getLockout().setLockDurationMinutes(10);
        LocalAccountLockoutPolicyLoader loader = new LocalAccountLockoutPolicyLoader(properties);

        LockoutPolicy policy = loader.getLockoutPolicy(UserTypeEnum.APP);

        assertEquals(UserTypeEnum.APP, policy.userType());
        assertTrue(policy.enabled());
        assertEquals(3, policy.maxAttempts());
        assertEquals(10, policy.lockDurationMinutes());
        assertEquals(15, policy.attemptWindowMinutes());
        assertEquals(3, policy.hintAfterAttempts());
    }
}
