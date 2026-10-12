package com.ingot.cloud.security.web.platform.security;

import com.ingot.cloud.security.authorization.SecurityGlobalAccess;
import com.ingot.cloud.security.model.domain.GatewayBlacklistEvent;
import com.ingot.cloud.security.model.domain.GatewayEndpointGroup;
import com.ingot.cloud.security.model.domain.GatewayIpList;
import com.ingot.cloud.security.model.domain.GatewayRateLimitRule;
import com.ingot.cloud.security.model.domain.GatewayViolationEscalation;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;
import com.ingot.cloud.security.service.policy.SecurityPolicyAdminService;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.HasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 安全策略中心管理 API（页面化）。
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RestController
@RequestMapping("/platform/security/policy")
@RequiredArgsConstructor
@Tag(name = "安全策略中心")
public class SecurityPolicyAPI implements RShortcuts {

    private final SecurityGlobalAccess access;

    private final SecurityPolicyAdminService policyService;

    // ====== endpoint groups ======

    /** 查询 API 路径分组；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/groups")
    @Operation(summary = "查询 API 路径分组")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ})
    public R<List<GatewayEndpointGroup>> listGroups() {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_READ);
        return ok(policyService.listGroups());
    }

    /** 新增 API 路径分组；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PostMapping("/groups")
    @Operation(summary = "新增 API 路径分组")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_CREATE})
    public R<Void> saveGroup(@RequestBody GatewayEndpointGroup group) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_CREATE);
        policyService.saveGroup(group);
        return ok();
    }

    /** 更新 API 路径分组；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PutMapping("/groups")
    @Operation(summary = "更新 API 路径分组")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_UPDATE})
    public R<Void> updateGroup(@RequestBody GatewayEndpointGroup group) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_UPDATE);
        policyService.updateGroup(group);
        return ok();
    }

    /** 删除 API 路径分组；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @DeleteMapping("/groups/{id}")
    @Operation(summary = "删除 API 路径分组")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_DELETE})
    public R<Void> deleteGroup(@PathVariable Long id) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_DELETE);
        policyService.deleteGroup(id);
        return ok();
    }

    // ====== rate limit rules ======

    /** 查询限流规则；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/rules")
    @Operation(summary = "查询限流规则")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ})
    public R<List<GatewayRateLimitRule>> listRules() {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_READ);
        return ok(policyService.listRules());
    }

    /** 新增限流规则；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PostMapping("/rules")
    @Operation(summary = "新增限流规则")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_CREATE})
    public R<Void> saveRule(@RequestBody GatewayRateLimitRule rule) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_CREATE);
        policyService.saveRule(rule);
        return ok();
    }

    /** 更新限流规则；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PutMapping("/rules")
    @Operation(summary = "更新限流规则")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_UPDATE})
    public R<Void> updateRule(@RequestBody GatewayRateLimitRule rule) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_UPDATE);
        policyService.updateRule(rule);
        return ok();
    }

    /** 删除限流规则；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除限流规则")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_DELETE})
    public R<Void> deleteRule(@PathVariable Long id) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_DELETE);
        policyService.deleteRule(id);
        return ok();
    }

    // ====== ip list ======

    /** 查询黑白名单；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/ip-list")
    @Operation(summary = "查询黑白名单")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ})
    public R<List<GatewayIpList>> listIpList() {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_READ);
        return ok(policyService.listIpList());
    }

    /** 新增黑白名单条目；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PostMapping("/ip-list")
    @Operation(summary = "新增黑白名单条目")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_CREATE})
    public R<Void> saveIpList(@RequestBody GatewayIpList item) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_CREATE);
        policyService.saveIpList(item);
        return ok();
    }

    /** 更新黑白名单条目；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PutMapping("/ip-list")
    @Operation(summary = "更新黑白名单条目")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_UPDATE})
    public R<Void> updateIpList(@RequestBody GatewayIpList item) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_UPDATE);
        policyService.updateIpList(item);
        return ok();
    }

    /** 删除黑白名单条目；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @DeleteMapping("/ip-list/{id}")
    @Operation(summary = "删除黑白名单条目")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_DELETE})
    public R<Void> deleteIpList(@PathVariable Long id) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_DELETE);
        policyService.deleteIpList(id);
        return ok();
    }

    // ====== blacklist event audit ======

    /** 查询封禁审计；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/events")
    @Operation(summary = "查询封禁审计")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ})
    public R<List<GatewayBlacklistEvent>> listEvents(@RequestParam(defaultValue = "100") int limit) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_READ);
        return ok(policyService.listEvents(limit));
    }

    // ====== challenge policies ======

    /** 查询挑战策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/challenges")
    @Operation(summary = "查询挑战策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ})
    public R<List<SecurityChallengePolicy>> listChallengePolicies() {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_READ);
        return ok(policyService.listChallengePolicies());
    }

    /** 新增挑战策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PostMapping("/challenges")
    @Operation(summary = "新增挑战策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_CREATE})
    public R<Void> saveChallengePolicy(@RequestBody SecurityChallengePolicy policy) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_CREATE);
        policyService.saveChallengePolicy(policy);
        return ok();
    }

    /** 更新挑战策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PutMapping("/challenges")
    @Operation(summary = "更新挑战策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_UPDATE})
    public R<Void> updateChallengePolicy(@RequestBody SecurityChallengePolicy policy) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_UPDATE);
        policyService.updateChallengePolicy(policy);
        return ok();
    }

    /** 删除挑战策略；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @DeleteMapping("/challenges/{id}")
    @Operation(summary = "删除挑战策略")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_DELETE})
    public R<Void> deleteChallengePolicy(@PathVariable Long id) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_DELETE);
        policyService.deleteChallengePolicy(id);
        return ok();
    }

    // ====== violation escalation ======

    /** 查询限流违规升级配置；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @GetMapping("/violation-escalation")
    @Operation(summary = "查询限流违规升级配置")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_READ})
    public R<GatewayViolationEscalation> getViolationEscalation() {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_READ);
        return ok(policyService.getViolationEscalation());
    }

    /** 更新限流违规升级配置；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PutMapping("/violation-escalation")
    @Operation(summary = "更新限流违规升级配置")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_UPDATE})
    public R<Void> saveViolationEscalation(@RequestBody GatewayViolationEscalation config) {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_UPDATE);
        policyService.saveViolationEscalation(config);
        return ok();
    }

    // ====== force refresh ======

    /** 强制广播全量失效；执行前校验平台身份、精确操作及有效 ALL 范围。 */
    @PostMapping("/broadcast-invalidation")
    @Operation(summary = "强制广播全量失效")
    @HasAnyAuthority({IamAction.VALUE_PLATFORM_SECURITY_POLICY_UPDATE})
    public R<Void> broadcastInvalidationAll() {
        access.require(IamAction.PLATFORM_SECURITY_POLICY_UPDATE);
        policyService.broadcastInvalidationAll();
        return ok();
    }
}
