package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.organization.DepartmentService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DepartmentDraft;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentUpdateInput;
import com.ingot.framework.commons.model.iam.PageResponse;
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
 * <p>提供当前租户部门树维护，拒绝跨租户引用和非空删除。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户部门")
@RequestMapping("/v1/tenant/departments")
@RequiredArgsConstructor
public class TenantDepartmentAPI implements RShortcuts {
    private final DepartmentService departments;

    /**
     * 列出部门树分页。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 部门页
     */
    @Operation(summary = "部门树")
    @GetMapping
    public R<PageResponse<ResourceDetail<DepartmentRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(departments.list(page, pageSize));
    }

    /**
     * 创建部门。
     *
     * @param input 部门草稿
     * @return 新部门 ID
     */
    @Operation(summary = "创建部门")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody DepartmentDraft input) {
        return ok(departments.create(input));
    }

    /**
     * 读取部门详情。
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<DepartmentRecord>> get(@PathVariable String id) {
        return ok(departments.get(id));
    }

    /**
     * 更新部门资料或位置。
     *
     * @param id 部门 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    public R<ResourceDetail<DepartmentRecord>> update(@PathVariable String id,
                                                      @Valid @RequestBody DepartmentUpdateInput input) {
        return ok(departments.update(id, input));
    }

    /**
     * 删除空部门。
     *
     * @param id 部门 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除空部门")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(departments.delete(id));
    }
}
