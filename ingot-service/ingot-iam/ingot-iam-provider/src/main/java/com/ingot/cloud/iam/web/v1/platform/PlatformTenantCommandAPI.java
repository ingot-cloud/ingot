package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.identity.TenantInitializationService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.cloud.iam.tenant.TenantQueryService;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.TenantCreateInput;
import com.ingot.framework.commons.model.iam.TenantPreviewResult;
import com.ingot.framework.commons.model.iam.TenantRecord;
import com.ingot.framework.commons.model.iam.TenantUpdateInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供平台组织创建、列表与实体更新，不返回租户业务成员或授权快照。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台组织命令")
@RequestMapping("/v1/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantCommandAPI implements RShortcuts {
    private final TenantInitializationService tenants;
    private final TenantQueryService queries;

    /**
     * 分页列出平台组织实体。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 组织名称包含匹配，可空
     * @param status 启停状态，可空；仅接受 ENABLED/DISABLED
     * @return 组织页
     */
    @Operation(summary = "组织列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<TenantRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return ok(queries.list(page, pageSize, name, status));
    }

    /**
     * 预览组织初始化结果，无写入。
     *
     * @param input 组织资料与所有者账号
     * @return 服务器目录解析后的预览
     */
    @Operation(summary = "预览组织初始化")
    @PostMapping("/preview")
    public R<Preview<TenantPreviewResult>> preview(@Valid @RequestBody TenantCreateInput input) {
        return ok(tenants.preview(input));
    }

    /**
     * 原子创建组织及最小引用。
     *
     * @param input 组织资料与所有者账号
     * @return 新组织 ID 与版本
     */
    @Operation(summary = "原子创建组织")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody TenantCreateInput input) {
        return ok(tenants.create(input));
    }

    /**
     * 读取平台组织实体。
     *
     * @param id 组织 ID
     * @return 组织详情
     */
    @Operation(summary = "组织实体")
    @GetMapping("/{id}")
    public R<ResourceDetail<TenantRecord>> get(@PathVariable String id) {
        return ok(queries.get(id));
    }

    /**
     * 更新平台可见组织实体。
     *
     * @param id 组织 ID
     * @param input 名称、头像与启停
     * @return 更新后详情
     */
    @Operation(summary = "更新组织实体")
    @PatchMapping("/{id}")
    public R<ResourceDetail<TenantRecord>> patch(@PathVariable String id, @Valid @RequestBody TenantUpdateInput input) {
        return ok(queries.patch(id, input));
    }
}
