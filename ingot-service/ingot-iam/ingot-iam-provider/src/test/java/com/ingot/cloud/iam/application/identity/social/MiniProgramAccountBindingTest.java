package com.ingot.cloud.iam.application.identity.social;

import com.ingot.cloud.iam.domain.identity.AccountSocialPrincipal;
import com.ingot.cloud.iam.infrastructure.persistence.entity.AccountSocialBindingEntity;
import com.ingot.cloud.iam.infrastructure.persistence.store.SocialConfigStore;
import com.ingot.cloud.iam.infrastructure.persistence.store.AccountSocialBindingStore;
import com.ingot.framework.social.wechat.properties.SocialWechatProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>验证社交登录和绑定使用全局账号ID，不再需要旧用户实体或成员ID。</p>
 * @author jy
 * @since 1.0.0
 */
class MiniProgramAccountBindingTest {
    private final AccountSocialBindingStore bindings = mock(AccountSocialBindingStore.class);
    private final MiniProgramUserSocialResolver resolver = new MiniProgramUserSocialResolver(bindings,
            mock(SocialConfigStore.class), new SocialWechatProperties());

    @Test
    void resolvesBoundAccountAndReturnsNullWhenUnbound() {
        AccountSocialBindingEntity row = new AccountSocialBindingEntity();
        row.setUserId(901L);
        when(bindings.getOne(any())).thenReturn(row, (AccountSocialBindingEntity) null);
        assertEquals(new AccountSocialPrincipal(901L), resolver.getUserInfo("test-open-id"));
        assertNull(resolver.getUserInfo("unbound-test-open-id"));
    }

    @Test
    void writesAccountIdForANewSocialBinding() {
        resolver.bind(new AccountSocialPrincipal(901L), "test-open-id");
        ArgumentCaptor<AccountSocialBindingEntity> saved = ArgumentCaptor.forClass(AccountSocialBindingEntity.class);
        verify(bindings).save(saved.capture());
        assertEquals(901L, saved.getValue().getUserId());
        assertEquals("test-open-id", saved.getValue().getUniqueId());
        assertNotNull(saved.getValue().getBindAt());
    }
}
