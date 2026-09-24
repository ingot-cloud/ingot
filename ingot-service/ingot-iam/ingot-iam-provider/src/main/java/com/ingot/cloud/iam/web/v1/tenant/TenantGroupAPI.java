package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.group.GroupService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GroupDraft;
import com.ingot.framework.commons.model.iam.GroupRecord;
import com.ingot.framework.commons.model.iam.GroupUpdateInput;
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
 * <p>提供租户静态用户组，修改后重验委派派生授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户用户组")
@RequestMapping("/v1/tenant/groups")
@RequiredArgsConstructor
public class TenantGroupAPI implements RShortcuts {
    private final GroupService groups;

    /**
     * 分页列出租户用户组。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 组名包含匹配，可空
     * @return 组页
     */
    @Operation(summary = "用户组列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<GroupRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name) {
        return ok(groups.list(AuthorizationDomain.TENANT, page, pageSize, name));
    }

    /**
     * 创建租户用户组。
     *
     * @param input 组草稿
     * @return 新组 ID
     */
    @Operation(summary = "创建用户组")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody GroupDraft input) {
        return ok(groups.create(AuthorizationDomain.TENANT, input));
    }

    /**
     * 读取租户用户组。
     *
     * @param id 组 ID
     * @return 组详情
     */
    @Operation(summary = "用户组详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<GroupRecord>> get(@PathVariable String id) {
        return ok(groups.get(AuthorizationDomain.TENANT, id));
    }

    /**
     * 替换租户用户组。
     *
     * @param id 组 ID
     * @param input 完整内容
     * @return 替换后详情
     */
    @Operation(summary = "替换用户组")
    @PutMapping("/{id}")
    public R<ResourceDetail<GroupRecord>> replace(@PathVariable String id, @Valid @RequestBody GroupUpdateInput input) {
        return ok(groups.replace(AuthorizationDomain.TENANT, id, input));
    }

    /**
     * 删除未被引用的租户用户组。
     *
     * @param id 组 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除未引用用户组")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(groups.delete(AuthorizationDomain.TENANT, id));
    }

    /**
     * 预览组引用影响，无写入。
     *
     * @param id 组 ID
     * @param input 待保存内容
     * @return 引用影响
     */
    @Operation(summary = "预览组引用影响")
    @PostMapping("/{id}/preview")
    public R<Preview<ReferenceImpactPreview>> preview(@PathVariable String id,
                                                      @Valid @RequestBody GroupUpdateInput input) {
        return ok(groups.preview(AuthorizationDomain.TENANT, id, input));
    }
}
