package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.delegation.DelegationService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DelegationInput;
import com.ingot.framework.commons.model.iam.DelegationRecord;
import com.ingot.framework.commons.model.iam.DelegationUpdateInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ReferenceImpactPreview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供租户委派限制，来源撤销使派生授权无效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户委派")
@RequestMapping("/v1/tenant/delegations")
@RequiredArgsConstructor
public class TenantDelegationAPI implements RShortcuts {
    private final DelegationService delegations;

    /**
     * 分页列出租户委派。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 委派页
     */
    @Operation(summary = "委派列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<DelegationRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(delegations.list(AuthorizationDomain.TENANT, page, pageSize));
    }

    /**
     * 创建租户委派。
     *
     * @param input 委派限制
     * @return 新委派 ID
     */
    @Operation(summary = "创建委派")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody DelegationInput input) {
        return ok(delegations.create(AuthorizationDomain.TENANT, input));
    }

    /**
     * 读取租户委派。
     *
     * @param id 委派 ID
     * @return 委派详情
     */
    @Operation(summary = "委派详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<DelegationRecord>> get(@PathVariable String id) {
        return ok(delegations.get(AuthorizationDomain.TENANT, id));
    }

    /**
     * 调整租户委派。
     *
     * @param id 委派 ID
     * @param input 完整限制
     * @return 更新后详情
     */
    @Operation(summary = "调整委派")
    @PutMapping("/{id}")
    public R<ResourceDetail<DelegationRecord>> replace(@PathVariable String id,
                                                       @Valid @RequestBody DelegationUpdateInput input) {
        return ok(delegations.replace(AuthorizationDomain.TENANT, id, input));
    }

    /**
     * 撤销租户委派。
     *
     * @param id 委派 ID
     * @return 撤销前版本
     */
    @Operation(summary = "撤销委派")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(delegations.delete(AuthorizationDomain.TENANT, id));
    }

    /**
     * 预览委派收缩影响，无写入。
     *
     * @param id 委派 ID
     * @param input 待保存限制
     * @return 引用影响
     */
    @Operation(summary = "预览委派收缩影响")
    @PostMapping("/{id}/preview")
    public R<Preview<ReferenceImpactPreview>> preview(@PathVariable String id,
                                                      @Valid @RequestBody DelegationUpdateInput input) {
        return ok(delegations.preview(AuthorizationDomain.TENANT, id, input));
    }
}
