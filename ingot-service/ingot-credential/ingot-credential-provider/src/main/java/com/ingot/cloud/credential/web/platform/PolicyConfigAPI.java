package com.ingot.cloud.credential.web.platform;

import java.util.List;

import com.ingot.cloud.credential.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.credential.service.PolicyConfigService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 策略配置管理 API
 *
 * @author jymot
 * @since 2026-01-22
 */
@Slf4j
@RestController
@RequestMapping("/policy-config")
@RequiredArgsConstructor
@Tag(name = "策略配置管理")
public class PolicyConfigAPI {
    private final PolicyConfigService policyConfigService;
    private final CredentialPolicyLoader policyLoader;

    /**
     * 获取租户的策略配置
     */
    @GetMapping("/{tenantId}/{policyType}")
    @Operation(summary = "获取策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:query"})
    public R<CredentialPolicyConfig> getPolicyConfig(
            @PathVariable Long tenantId,
            @PathVariable String policyType) {
        try {
            CredentialPolicyConfig config = policyConfigService.getPolicyConfig(tenantId, policyType);
            return R.ok(config);
        } catch (Exception e) {
            log.error("获取策略配置失败", e);
            return R.error500("获取策略配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取租户的所有策略配置
     */
    @GetMapping("/list/{tenantId}")
    @Operation(summary = "获取所有策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:query"})
    public R<List<CredentialPolicyConfig>> getAllPolicyConfigs(@PathVariable Long tenantId) {
        try {
            List<CredentialPolicyConfig> configs = policyConfigService.getAllPolicyConfigs(tenantId);
            return R.ok(configs);
        } catch (Exception e) {
            log.error("获取策略配置列表失败", e);
            return R.error500("获取策略配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取全局默认策略配置
     */
    @GetMapping("/global")
    @Operation(summary = "获取全局默认策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:query"})
    public R<List<CredentialPolicyConfig>> getGlobalPolicyConfigs() {
        try {
            List<CredentialPolicyConfig> configs = policyConfigService.getAllPolicyConfigs(null);
            return R.ok(configs);
        } catch (Exception e) {
            log.error("获取全局策略配置失败", e);
            return R.error500("获取全局策略配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存或更新策略配置
     */
    @PostMapping
    @Operation(summary = "保存或更新策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:write"})
    public R<CredentialPolicyConfig> savePolicyConfig(@RequestBody CredentialPolicyConfig config) {
        try {
            CredentialPolicyConfig saved = policyConfigService.savePolicyConfig(config);

            // 刷新缓存
            policyConfigService.refreshCache(saved.getTenantId(), saved.getPolicyType());
            policyLoader.reloadPolicies(saved.getTenantId());

            return R.ok(saved);
        } catch (Exception e) {
            log.error("保存策略配置失败", e);
            return R.error500("保存策略配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除策略配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:delete"})
    public R<Void> deletePolicyConfig(@PathVariable Long id) {
        try {
            policyConfigService.deletePolicyConfig(id);
            return R.ok();
        } catch (Exception e) {
            log.error("删除策略配置失败", e);
            return R.error500("删除策略配置失败: " + e.getMessage());
        }
    }

    /**
     * 刷新策略缓存
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新策略缓存")
    @AdminOrHasAnyAuthority({"platform:credential:policy:refresh"})
    public R<Void> refreshCache(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String policyType) {
        try {
            if (tenantId == null && policyType == null) {
                // 清空所有缓存
                policyConfigService.clearAllCache();
                policyLoader.clearPolicyCache();
                log.info("已清空所有策略缓存");
            } else {
                // 刷新指定缓存
                policyConfigService.refreshCache(tenantId, CredentialPolicyType.getEnum(policyType));
                policyLoader.reloadPolicies(tenantId);
                log.info("已刷新策略缓存 - 租户ID: {}, 策略类型: {}", tenantId, policyType);
            }
            return R.ok();
        } catch (Exception e) {
            log.error("刷新缓存失败", e);
            return R.error500("刷新缓存失败: " + e.getMessage());
        }
    }
}
