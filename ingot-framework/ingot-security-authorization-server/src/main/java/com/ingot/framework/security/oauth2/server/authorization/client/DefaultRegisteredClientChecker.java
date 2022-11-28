package com.ingot.framework.security.oauth2.server.authorization.client;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * <p>Description  : DefaultRegisteredClientChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 7:18 PM.</p>
 */
@Slf4j
public class DefaultRegisteredClientChecker implements RegisteredClientChecker {

    @Override
    public void check(RegisteredClient client) {
        String status = RegisteredClientOps.of(client).getClientStatus();
        if (!StrUtil.equals(status, CommonStatusEnum.ENABLE.getValue())) {
            log.debug("[DefaultRegisteredClientChecker] 客户端[{}] 不可用，状态[{}]",
                    client.getClientName(), status);
            throw new LockedException("无法登录该客户端");
        }
    }
}
