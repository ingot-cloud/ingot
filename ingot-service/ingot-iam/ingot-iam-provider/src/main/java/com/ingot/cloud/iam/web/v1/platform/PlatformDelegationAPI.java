package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.delegation.JdbcDelegationService;
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
 * <p>提供平台委派限制，人群不得引用部门。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台委派")
@RequestMapping("/v1/platform/delegations")
@RequiredArgsConstructor
public class PlatformDelegationAPI implements RShortcuts {
    private final JdbcDelegationService delegations;

    /**
     * 分页列出平台委派。
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
        return ok(delegations.list(AuthorizationDomain.PLATFORM, page, pageSize));
    }

    /**
     * 创建平台委派。
     *
     * @param input 委派限制
     * @return 新委派 ID
     */
    @Operation(summary = "创建委派")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody DelegationInput input) {
        return ok(delegations.create(AuthorizationDomain.PLATFORM, input));
    }

    /**
     * 读取平台委派。
     *
     * @param id 委派 ID
     * @return 委派详情
     */
    @Operation(summary = "委派详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<DelegationRecord>> get(@PathVariable String id) {
        return ok(delegations.get(AuthorizationDomain.PLATFORM, id));
    }

    /**
     * 调整平台委派。
     *
     * @param id 委派 ID
     * @param input 完整限制
     * @return 更新后详情
     */
    @Operation(summary = "调整委派")
    @PutMapping("/{id}")
    public R<ResourceDetail<DelegationRecord>> replace(@PathVariable String id,
                                                       @Valid @RequestBody DelegationUpdateInput input) {
        return ok(delegations.replace(AuthorizationDomain.PLATFORM, id, input));
    }

    /**
     * 撤销平台委派。
     *
     * @param id 委派 ID
     * @return 撤销前版本
     */
    @Operation(summary = "撤销委派")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(delegations.delete(AuthorizationDomain.PLATFORM, id));
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
        return ok(delegations.preview(AuthorizationDomain.PLATFORM, id, input));
    }
}
