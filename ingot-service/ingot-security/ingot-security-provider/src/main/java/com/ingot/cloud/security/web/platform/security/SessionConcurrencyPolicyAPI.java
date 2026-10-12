package com.ingot.cloud.security.web.platform.security;

import java.util.List;

import com.ingot.cloud.security.authorization.SecurityGlobalAccess;
import com.ingot.cloud.security.model.domain.SessionConcurrencyPolicy;
import com.ingot.cloud.security.service.session.SessionConcurrencyPolicyAdminService;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.HasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>并发会话策略 Platform API，供安全中心页面维护「最多几个会话、超限怎么处置」。</p>
 *
 * <p>策略变更后由服务层广播失效，Auth 各实例在下一次登录前生效，无需重启；
 * 已在线的会话不受策略调整影响，收敛发生在下一次登录。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/platform/security/session/concurrency-policies")
@RequiredArgsConstructor
@Tag(name = "安全中心")
public class SessionConcurrencyPolicyAPI implements RShortcuts {

    private final SecurityGlobalAccess access;

    private final SessionConcurrencyPolicyAdminService adminService;

    /** 查询并发会话策略列表；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping
    @Operation(summary = "查询并发会话策略列表")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SESSION_POLICY_READ})
    public R<List<SessionConcurrencyPolicy>> list() {
        access.require(IamAction.PLATFORM_SESSION_POLICY_READ);
        return ok(adminService.list());
    }

    /** 查询并发会话策略详情；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询并发会话策略详情")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SESSION_POLICY_READ})
    public R<SessionConcurrencyPolicy> getById(@PathVariable Long id) {
        access.require(IamAction.PLATFORM_SESSION_POLICY_READ);
        return ok(adminService.getById(id));
    }

    /** 新增并发会话策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PostMapping
    @Operation(summary = "新增并发会话策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SESSION_POLICY_UPDATE})
    public R<SessionConcurrencyPolicy> create(@RequestBody SessionConcurrencyPolicy policy) {
        access.require(IamAction.PLATFORM_SESSION_POLICY_UPDATE);
        return ok(adminService.create(policy));
    }

    /** 更新并发会话策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PutMapping
    @Operation(summary = "更新并发会话策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SESSION_POLICY_UPDATE})
    public R<SessionConcurrencyPolicy> update(@RequestBody SessionConcurrencyPolicy policy) {
        access.require(IamAction.PLATFORM_SESSION_POLICY_UPDATE);
        return ok(adminService.update(policy));
    }

    /** 删除并发会话策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除并发会话策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SESSION_POLICY_UPDATE})
    public R<Void> delete(@PathVariable Long id) {
        access.require(IamAction.PLATFORM_SESSION_POLICY_UPDATE);
        adminService.delete(id);
        return ok();
    }
}
