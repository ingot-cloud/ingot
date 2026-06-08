package com.ingot.cloud.security.web.platform.security;

import com.ingot.cloud.security.model.domain.GatewayBlacklistEvent;
import com.ingot.cloud.security.model.domain.GatewayEndpointGroup;
import com.ingot.cloud.security.model.domain.GatewayIpList;
import com.ingot.cloud.security.model.domain.GatewayRateLimitRule;
import com.ingot.cloud.security.model.domain.GatewayViolationEscalation;
import com.ingot.cloud.security.model.domain.SecurityChallengePolicy;
import com.ingot.cloud.security.service.policy.SecurityPolicyAdminService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
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

    private final SecurityPolicyAdminService policyService;

    // ====== endpoint groups ======

    @GetMapping("/groups")
    @Operation(summary = "查询 API 路径分组")
    @AdminOrHasAnyAuthority({"platform:security:policy:query"})
    public R<List<GatewayEndpointGroup>> listGroups() {
        return ok(policyService.listGroups());
    }

    @PostMapping("/groups")
    @Operation(summary = "新增 API 路径分组")
    @AdminOrHasAnyAuthority({"platform:security:policy:create"})
    public R<Void> saveGroup(@RequestBody GatewayEndpointGroup group) {
        policyService.saveGroup(group);
        return ok();
    }

    @PutMapping("/groups")
    @Operation(summary = "更新 API 路径分组")
    @AdminOrHasAnyAuthority({"platform:security:policy:update"})
    public R<Void> updateGroup(@RequestBody GatewayEndpointGroup group) {
        policyService.updateGroup(group);
        return ok();
    }

    @DeleteMapping("/groups/{id}")
    @Operation(summary = "删除 API 路径分组")
    @AdminOrHasAnyAuthority({"platform:security:policy:delete"})
    public R<Void> deleteGroup(@PathVariable Long id) {
        policyService.deleteGroup(id);
        return ok();
    }

    // ====== rate limit rules ======

    @GetMapping("/rules")
    @Operation(summary = "查询限流规则")
    @AdminOrHasAnyAuthority({"platform:security:policy:query"})
    public R<List<GatewayRateLimitRule>> listRules() {
        return ok(policyService.listRules());
    }

    @PostMapping("/rules")
    @Operation(summary = "新增限流规则")
    @AdminOrHasAnyAuthority({"platform:security:policy:create"})
    public R<Void> saveRule(@RequestBody GatewayRateLimitRule rule) {
        policyService.saveRule(rule);
        return ok();
    }

    @PutMapping("/rules")
    @Operation(summary = "更新限流规则")
    @AdminOrHasAnyAuthority({"platform:security:policy:update"})
    public R<Void> updateRule(@RequestBody GatewayRateLimitRule rule) {
        policyService.updateRule(rule);
        return ok();
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除限流规则")
    @AdminOrHasAnyAuthority({"platform:security:policy:delete"})
    public R<Void> deleteRule(@PathVariable Long id) {
        policyService.deleteRule(id);
        return ok();
    }

    // ====== ip list ======

    @GetMapping("/ip-list")
    @Operation(summary = "查询黑白名单")
    @AdminOrHasAnyAuthority({"platform:security:policy:query"})
    public R<List<GatewayIpList>> listIpList() {
        return ok(policyService.listIpList());
    }

    @PostMapping("/ip-list")
    @Operation(summary = "新增黑白名单条目")
    @AdminOrHasAnyAuthority({"platform:security:policy:create"})
    public R<Void> saveIpList(@RequestBody GatewayIpList item) {
        policyService.saveIpList(item);
        return ok();
    }

    @PutMapping("/ip-list")
    @Operation(summary = "更新黑白名单条目")
    @AdminOrHasAnyAuthority({"platform:security:policy:update"})
    public R<Void> updateIpList(@RequestBody GatewayIpList item) {
        policyService.updateIpList(item);
        return ok();
    }

    @DeleteMapping("/ip-list/{id}")
    @Operation(summary = "删除黑白名单条目")
    @AdminOrHasAnyAuthority({"platform:security:policy:delete"})
    public R<Void> deleteIpList(@PathVariable Long id) {
        policyService.deleteIpList(id);
        return ok();
    }

    // ====== blacklist event audit ======

    @GetMapping("/events")
    @Operation(summary = "查询封禁审计")
    @AdminOrHasAnyAuthority({"platform:security:policy:query"})
    public R<List<GatewayBlacklistEvent>> listEvents(@RequestParam(defaultValue = "100") int limit) {
        return ok(policyService.listEvents(limit));
    }

    // ====== challenge policies ======

    @GetMapping("/challenges")
    @Operation(summary = "查询挑战策略")
    @AdminOrHasAnyAuthority({"platform:security:policy:query"})
    public R<List<SecurityChallengePolicy>> listChallengePolicies() {
        return ok(policyService.listChallengePolicies());
    }

    @PostMapping("/challenges")
    @Operation(summary = "新增挑战策略")
    @AdminOrHasAnyAuthority({"platform:security:policy:create"})
    public R<Void> saveChallengePolicy(@RequestBody SecurityChallengePolicy policy) {
        policyService.saveChallengePolicy(policy);
        return ok();
    }

    @PutMapping("/challenges")
    @Operation(summary = "更新挑战策略")
    @AdminOrHasAnyAuthority({"platform:security:policy:update"})
    public R<Void> updateChallengePolicy(@RequestBody SecurityChallengePolicy policy) {
        policyService.updateChallengePolicy(policy);
        return ok();
    }

    @DeleteMapping("/challenges/{id}")
    @Operation(summary = "删除挑战策略")
    @AdminOrHasAnyAuthority({"platform:security:policy:delete"})
    public R<Void> deleteChallengePolicy(@PathVariable Long id) {
        policyService.deleteChallengePolicy(id);
        return ok();
    }

    // ====== violation escalation ======

    @GetMapping("/violation-escalation")
    @Operation(summary = "查询限流违规升级配置")
    @AdminOrHasAnyAuthority({"platform:security:policy:query"})
    public R<GatewayViolationEscalation> getViolationEscalation() {
        return ok(policyService.getViolationEscalation());
    }

    @PutMapping("/violation-escalation")
    @Operation(summary = "更新限流违规升级配置")
    @AdminOrHasAnyAuthority({"platform:security:policy:update"})
    public R<Void> saveViolationEscalation(@RequestBody GatewayViolationEscalation config) {
        policyService.saveViolationEscalation(config);
        return ok();
    }

    // ====== force refresh ======

    @PostMapping("/broadcast-invalidation")
    @Operation(summary = "强制广播全量失效")
    @AdminOrHasAnyAuthority({"platform:security:policy:update"})
    public R<Void> broadcastInvalidationAll() {
        policyService.broadcastInvalidationAll();
        return ok();
    }
}
