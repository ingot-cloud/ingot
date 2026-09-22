package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.catalog.CatalogService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.ActionDraft;
import com.ingot.framework.commons.model.iam.ActionRecord;
import com.ingot.framework.commons.model.iam.ActionUpdateInput;
import com.ingot.framework.commons.model.iam.ApplicationDraft;
import com.ingot.framework.commons.model.iam.ApplicationRecord;
import com.ingot.framework.commons.model.iam.ApplicationUpdateInput;
import com.ingot.framework.commons.model.iam.ConfigurationStatusInput;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.CatalogListView;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MenuDraft;
import com.ingot.framework.commons.model.iam.MenuRecord;
import com.ingot.framework.commons.model.iam.MenuUpdateInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.ResourceDraft;
import com.ingot.framework.commons.model.iam.ResourceRecord;
import com.ingot.framework.commons.model.iam.ResourceUpdateInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供平台应用、资源、操作与菜单目录，不把启用状态当作业务授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台应用目录")
@RequestMapping("/v1/platform/applications")
@RequiredArgsConstructor
public class PlatformApplicationAPI implements RShortcuts {
    private final CatalogService catalog;

    /**
     * 分页列出应用目录。
     *
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     * @param name 应用名称包含匹配，可空
     * @param status 启停状态，可空；仅接受 ENABLED/DISABLED
     * @param baseline 是否组织默认开通，可空
     * @return 应用页
     */
    @Operation(summary = "应用目录")
    @GetMapping
    public R<PageResponse<ResourceDetail<ApplicationRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean baseline) {
        return ok(catalog.listApplications(page, pageSize, name, status, baseline));
    }

    /**
     * 创建应用目录项，不自动开通任何租户。
     *
     * @param input 应用草稿
     * @return 新应用 ID
     */
    @Operation(summary = "创建应用")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody ApplicationDraft input) {
        return ok(catalog.createApplication(input));
    }

    /**
     * 读取应用详情。
     *
     * @param id 应用 ID
     * @return 应用详情
     */
    @Operation(summary = "应用详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<ApplicationRecord>> get(@PathVariable String id) {
        return ok(catalog.getApplication(id));
    }

    /**
     * 更新展示信息与基础开通标记。
     *
     * @param id 应用 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新应用")
    @PutMapping("/{id}")
    public R<ResourceDetail<ApplicationRecord>> update(@PathVariable String id,
                                                       @Valid @RequestBody ApplicationUpdateInput input) {
        return ok(catalog.updateApplication(id, input));
    }

    /**
     * 全局启停应用。
     *
     * @param id 应用 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    @Operation(summary = "启停应用")
    @PatchMapping("/{id}")
    public R<CreatedResource> changeStatus(@PathVariable String id,
                                           @Valid @RequestBody ConfigurationStatusInput input) {
        return ok(catalog.changeApplicationStatus(id, input));
    }

    /**
     * 删除未被引用的应用。
     *
     * @param id 应用 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除未引用应用")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(catalog.deleteApplication(id));
    }

    /**
     * 列出应用资源。
     *
     * @param id 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param name 资源名称包含匹配，可空
     * @param code 资源编码包含匹配，可空
     * @return 资源页
     */
    @Operation(summary = "应用资源")
    @GetMapping("/{id}/resources")
    public R<PageResponse<ResourceDetail<ResourceRecord>>> listResources(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code) {
        return ok(catalog.listResources(id, page, pageSize, name, code));
    }

    /**
     * 创建资源。
     *
     * @param id 应用 ID
     * @param input 资源草稿
     * @return 新资源 ID
     */
    @Operation(summary = "创建资源")
    @PostMapping("/{id}/resources")
    public R<CreatedResource> createResource(@PathVariable String id, @Valid @RequestBody ResourceDraft input) {
        return ok(catalog.createResource(id, input));
    }

    /**
     * 更新资源能力。
     *
     * @param id 应用 ID
     * @param resourceId 资源 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新资源")
    @PutMapping("/{id}/resources/{resourceId}")
    public R<ResourceDetail<ResourceRecord>> updateResource(@PathVariable String id, @PathVariable String resourceId,
                                                            @Valid @RequestBody ResourceUpdateInput input) {
        return ok(catalog.updateResource(id, resourceId, input));
    }

    /**
     * 删除未被操作引用的资源。
     *
     * @param id 应用 ID
     * @param resourceId 资源 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除未引用资源")
    @DeleteMapping("/{id}/resources/{resourceId}")
    public R<CreatedResource> deleteResource(@PathVariable String id, @PathVariable String resourceId) {
        return ok(catalog.deleteResource(id, resourceId));
    }

    /**
     * 列出精确操作。
     *
     * @param id 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param resourceId 所属资源，可空
     * @param name 操作名称包含匹配，可空
     * @param ids 逗号分隔操作 ID，可空
     * @return 操作页
     */
    @Operation(summary = "应用操作")
    @GetMapping("/{id}/actions")
    public R<PageResponse<ResourceDetail<ActionRecord>>> listActions(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ids) {
        return ok(catalog.listActions(id, page, pageSize, resourceId, name, ids));
    }

    /**
     * 创建精确操作。
     *
     * @param id 应用 ID
     * @param input 操作草稿
     * @return 新操作 ID
     */
    @Operation(summary = "创建操作")
    @PostMapping("/{id}/actions")
    public R<CreatedResource> createAction(@PathVariable String id, @Valid @RequestBody ActionDraft input) {
        return ok(catalog.createAction(id, input));
    }

    /**
     * 更新操作名称。
     *
     * @param id 应用 ID
     * @param actionId 操作 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新操作")
    @PutMapping("/{id}/actions/{actionId}")
    public R<ResourceDetail<ActionRecord>> updateAction(@PathVariable String id, @PathVariable String actionId,
                                                        @Valid @RequestBody ActionUpdateInput input) {
        return ok(catalog.updateAction(id, actionId, input));
    }

    /**
     * 启停操作。
     *
     * @param id 应用 ID
     * @param actionId 操作 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    @Operation(summary = "启停操作")
    @PatchMapping("/{id}/actions/{actionId}")
    public R<CreatedResource> changeActionStatus(@PathVariable String id, @PathVariable String actionId,
                                                 @Valid @RequestBody ConfigurationStatusInput input) {
        return ok(catalog.changeActionStatus(id, actionId, input));
    }

    /**
     * 删除未被引用的操作。
     *
     * @param id 应用 ID
     * @param actionId 操作 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除未引用操作")
    @DeleteMapping("/{id}/actions/{actionId}")
    public R<CreatedResource> deleteAction(@PathVariable String id, @PathVariable String actionId) {
        return ok(catalog.deleteAction(id, actionId));
    }

    /**
     * 列出应用菜单。
     *
     * @param id 应用 ID
     * @param view page 返回分页，tree 返回整树
     * @param page 页码，tree 视图忽略
     * @param pageSize 页大小，tree 视图忽略
     * @return 菜单页或菜单树
     */
    @Operation(summary = "应用菜单")
    @GetMapping("/{id}/menus")
    public R<?> listMenus(
            @PathVariable String id,
            @RequestParam(defaultValue = CatalogListView.VALUE_PAGE) String view,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        CatalogListView listView = requireView(view);
        if (listView == CatalogListView.TREE) {
            return ok(catalog.listMenuTree(id));
        }
        return ok(catalog.listMenus(id, page, pageSize));
    }

    /**
     * 创建菜单。
     *
     * @param id 应用 ID
     * @param input 菜单草稿
     * @return 新菜单 ID
     */
    @Operation(summary = "创建菜单")
    @PostMapping("/{id}/menus")
    public R<CreatedResource> createMenu(@PathVariable String id, @Valid @RequestBody MenuDraft input) {
        return ok(catalog.createMenu(id, input));
    }

    /**
     * 更新菜单。
     *
     * @param id 应用 ID
     * @param menuId 菜单 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新菜单")
    @PutMapping("/{id}/menus/{menuId}")
    public R<ResourceDetail<MenuRecord>> updateMenu(@PathVariable String id, @PathVariable String menuId,
                                                    @Valid @RequestBody MenuUpdateInput input) {
        return ok(catalog.updateMenu(id, menuId, input));
    }

    /**
     * 删除无子节点菜单。
     *
     * @param id 应用 ID
     * @param menuId 菜单 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}/menus/{menuId}")
    public R<CreatedResource> deleteMenu(@PathVariable String id, @PathVariable String menuId) {
        return ok(catalog.deleteMenu(id, menuId));
    }

    private static CatalogListView requireView(String view) {
        try {
            return CatalogListView.getEnum(view);
        } catch (IllegalArgumentException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
