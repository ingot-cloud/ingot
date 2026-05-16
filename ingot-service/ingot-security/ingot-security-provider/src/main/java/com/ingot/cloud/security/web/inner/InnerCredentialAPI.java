package com.ingot.cloud.security.web.inner;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 凭证安全内部接口。
 * <p>
 * 走 {@link CredentialPolicyConfigService} 装饰器链：L1 Caffeine -> L2 Redis -> Local Mapper，
 * 命中 L1/L2 时不会触达数据库；写端事务提交后通过事件总线广播失效，确保所有副本一致。
 * </p>
 *
 * @author jymot
 * @since 2026-01-22
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping("/inner/credential")
@RequiredArgsConstructor
public class InnerCredentialAPI implements RShortcuts {
    private final CredentialPolicyConfigService credentialPolicyConfigService;

    /**
     * 获取密码策略列表
     */
    @GetMapping("/policy-configs")
    public R<List<CredentialPolicyConfigVO>> getPolicyConfigs() {
        return ok(credentialPolicyConfigService.getAll());
    }
}
