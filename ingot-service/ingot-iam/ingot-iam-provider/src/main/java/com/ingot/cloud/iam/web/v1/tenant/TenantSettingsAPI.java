package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.tenant.TenantQueryService;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.OwnerTransferInput;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.TenantRecord;
import com.ingot.framework.commons.model.iam.TenantSettingsInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供当前组织可编辑设置与所有者转交，不能通过设置移除最后所有者。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户设置")
@RequestMapping("/v1/tenant/settings")
@RequiredArgsConstructor
public class TenantSettingsAPI implements RShortcuts {
    private final TenantQueryService tenants;

    /**
     * 读取当前组织设置。
     *
     * @return 组织详情
     */
    @Operation(summary = "组织设置")
    @GetMapping
    public R<ResourceDetail<TenantRecord>> get() {
        return ok(tenants.settings());
    }

    /**
     * 更新当前组织可编辑设置。
     *
     * @param input 名称与头像
     * @return 更新后详情
     */
    @Operation(summary = "更新组织设置")
    @PutMapping
    public R<ResourceDetail<TenantRecord>> update(@Valid @RequestBody TenantSettingsInput input) {
        return ok(tenants.updateSettings(input));
    }

    /**
     * 原子转交所有者。
     *
     * @param input 新所有者
     * @return 提交后版本
     */
    @Operation(summary = "转交所有者")
    @PostMapping("/owner-transfer")
    public R<CreatedResource> transfer(@Valid @RequestBody OwnerTransferInput input) {
        return ok(tenants.transferOwner(input));
    }
}
