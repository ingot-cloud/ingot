package com.ingot.cloud.iam.web.v1;

import com.ingot.cloud.iam.policy.JdbcPolicyService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供普通通讯录搜索与详情，按策略投影且不返回管理 ACTION。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 普通通讯录")
@RequestMapping("/v1/directory")
@RequiredArgsConstructor
public class DirectoryAPI implements RShortcuts {
    private final JdbcPolicyService policies;

    /**
     * 分页列出可见通讯录成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 成员页
     */
    @Operation(summary = "普通通讯录成员")
    @GetMapping("/members")
    public R<PageResponse<ResourceDetail<MemberRecord>>> members(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(policies.listDirectoryMembers(page, pageSize));
    }

    /**
     * 读取可见通讯录成员详情。
     *
     * @param id 成员 ID
     * @return 投影详情
     */
    @Operation(summary = "普通通讯录成员详情")
    @GetMapping("/members/{id}")
    public R<ResourceDetail<MemberRecord>> member(@PathVariable String id) {
        return ok(policies.getDirectoryMember(id));
    }

    /**
     * 列出可见部门树。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 部门页
     */
    @Operation(summary = "普通通讯录部门树")
    @GetMapping("/departments")
    public R<PageResponse<ResourceDetail<DepartmentRecord>>> departments(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(policies.listDirectoryDepartments(page, pageSize));
    }
}
