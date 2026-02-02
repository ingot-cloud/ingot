package com.ingot.cloud.credential.web.platform;

import java.util.List;

import com.ingot.cloud.credential.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.credential.service.PolicyConfigService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
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
public class PolicyConfigAPI implements RShortcuts {
    private final PolicyConfigService policyConfigService;
    private final CredentialPolicyLoader policyLoader;

    /**
     * 获取策略配置
     */
    @GetMapping("/{policyType}")
    @Operation(summary = "获取策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:query"})
    public R<CredentialPolicyConfig> getPolicyConfig(
            @PathVariable String policyType) {
        try {
            CredentialPolicyConfig config = policyConfigService.getPolicyConfig(policyType);
            return ok(config);
        } catch (Exception e) {
            log.error("获取策略配置失败", e);
            return error("获取策略配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取租户的所有策略配置
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:query"})
    public R<List<CredentialPolicyConfig>> getAllPolicyConfigs() {
        try {
            List<CredentialPolicyConfig> configs = policyConfigService.getAllPolicyConfigs();
            return ok(configs);
        } catch (Exception e) {
            log.error("获取策略配置列表失败", e);
            return error("获取策略配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 保存策略配置
     */
    @PostMapping
    @Operation(summary = "保存策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:create"})
    public R<Void> savePolicyConfig(@RequestBody CredentialPolicyConfig config) {
        try {
            CredentialPolicyConfig saved = policyConfigService.savePolicyConfig(config);

            // 刷新缓存
            policyConfigService.refreshCache(saved.getPolicyType());
            policyLoader.clearPolicyCache();

            return ok();
        } catch (Exception e) {
            log.error("保存策略配置失败", e);
            return error("保存策略配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存或更新策略配置
     */
    @PostMapping
    @Operation(summary = "更新策略配置")
    @AdminOrHasAnyAuthority({"platform:credential:policy:update"})
    public R<Void> updatePolicyConfig(@RequestBody CredentialPolicyConfig config) {
        try {
            CredentialPolicyConfig updated = policyConfigService.updatePolicyConfig(config);

            // 刷新缓存
            policyConfigService.refreshCache(updated.getPolicyType());
            policyLoader.clearPolicyCache();

            return ok();
        } catch (Exception e) {
            log.error("保存策略配置失败", e);
            return error("保存策略配置失败: " + e.getMessage());
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
            policyLoader.clearPolicyCache();
            return ok();
        } catch (Exception e) {
            log.error("删除策略配置失败", e);
            return error("删除策略配置失败: " + e.getMessage());
        }
    }
}
