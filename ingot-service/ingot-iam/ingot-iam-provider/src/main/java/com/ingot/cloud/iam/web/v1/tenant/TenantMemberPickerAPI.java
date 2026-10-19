package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest;
import com.ingot.cloud.iam.organization.OrganizationMemberPickerService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.OrganizationMemberPickerPage;
import com.ingot.framework.commons.model.iam.OrganizationMemberPickerScene;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>供租户组织成员对话框使用的专用读接口，不改变其他场景的根成员语义。</p>
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 组织成员选择")
@RequestMapping("/v1/tenant/member-picker")
@RequiredArgsConstructor
public class TenantMemberPickerAPI implements RShortcuts {
    private final OrganizationMemberPickerService picker;

    /** 按场景资格一次返回部门分支与成员，两个列表分别按默认20条分页。 */
    @IamReadOnlyRequest
    @com.ingot.framework.commons.annotation.field.FieldControl(domain = com.ingot.framework.commons.model.iam.AuthorizationDomain.TENANT,
            applicationCode = com.ingot.framework.commons.model.iam.MemberResources.TENANT_APPLICATION,
            resourceCode = com.ingot.framework.commons.model.iam.MemberResources.MEMBER,
            action = com.ingot.framework.commons.model.iam.IamAction.VALUE_TENANT_MEMBER_READ,
            use = com.ingot.framework.commons.annotation.field.FieldUse.FILTER,
            valueType = com.ingot.framework.commons.model.iam.OrganizationMemberBrowseFilter.class)
    @GetMapping("/browse")
    @Operation(summary = "组织成员选择器浏览")
    public R<OrganizationMemberPickerPage> browse(@RequestParam OrganizationMemberPickerScene scene,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String targetDepartmentId,
            @RequestParam(required = false) String delegationGrantId,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int departmentPage,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int departmentPageSize) {
        return ok(picker.browse(scene, departmentId, keyword, targetDepartmentId, delegationGrantId,
                page, pageSize, departmentPage, departmentPageSize));
    }
}
