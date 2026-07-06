package com.ingot.framework.security.oauth2.server.authorization.client;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.security.core.InSecurityMessageSource;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * <p>Description  : DefaultRegisteredClientChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 7:18 PM.</p>
 */
@Slf4j
public class DefaultRegisteredClientChecker implements RegisteredClientChecker {
    private final MessageSourceAccessor messages = InSecurityMessageSource.getAccessor();

    @Override
    public void check(RegisteredClient client) {
        String status = RegisteredClientOps.of(client).getClientStatus();
        if (!StrUtil.equals(status, CommonStatusEnum.ENABLE.getValue())) {
            log.debug("[DefaultRegisteredClientChecker] 客户端[{}] 不可用，状态[{}]",
                    client.getClientName(), status);
            OAuth2ErrorUtils.throwNotAllowClient(this.messages
                    .getMessage("OAuth2UserDetailsAuthenticationProvider.notAllowClient",
                            "不允许访问客户端"));
        }
    }
}
