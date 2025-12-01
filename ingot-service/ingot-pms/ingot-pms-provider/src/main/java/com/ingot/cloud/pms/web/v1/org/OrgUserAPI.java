package com.ingot.cloud.pms.web.v1.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : OrgUserAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/22.</p>
 * <p>Time         : 1:55 PM.</p>
 */
@Slf4j
@Tag(description = "OrgUser", name = "组织用户模块")
@RestController
@RequestMapping(value = "/v1/org/user")
@RequiredArgsConstructor
public class OrgUserAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final BizUserService bizUserService;

    @Operation(summary = "组织用户分页", description = "组织用户分页")
    @AdminOrHasAnyAuthority({"contacts:member:query"})
    @GetMapping("/page")
    public R<?> page(Page<SysUser> page, UserQueryDTO condition) {
        Long tenantId = TenantContextHolder.get();
        return ok(sysUserService.conditionPage(page, condition, tenantId));
    }

    @Operation(summary = "组织用户分页", description = "组织用户分页")
    @AdminOrHasAnyAuthority({"contacts:member:query"})
    @GetMapping("/role/{roleId}/page")
    public R<?> pageWithRoleStatus(Page<SysUser> page,
                                   UserQueryDTO condition,
                                   @PathVariable Long roleId) {
        Long tenantId = TenantContextHolder.get();
        condition.setRoleId(null);
        return ok(bizUserService.conditionPageWithRole(page, condition, tenantId, roleId));
    }

    @Operation(summary = "组织用户创建", description = "组织用户创建")
    @AdminOrHasAnyAuthority({"contacts:member:create"})
    @PostMapping
    public R<?> create(@RequestBody OrgUserDTO params) {
        bizUserService.orgCreateUser(params);
        return ok();
    }

    @Operation(summary = "组织用户更新", description = "组织用户更新")
    @AdminOrHasAnyAuthority({"contacts:member:update"})
    @PutMapping
    public R<?> update(@RequestBody OrgUserDTO params) {
        bizUserService.orgUpdateUser(params);
        return ok();
    }

    @Operation(summary = "组织用户删除", description = "组织用户删除")
    @AdminOrHasAnyAuthority({"contacts:member:delete"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizUserService.orgDeleteUser(id);
        return ok();
    }

    @Operation(summary = "组织用户详情", description = "组织用户详情")
    @AdminOrHasAnyAuthority({"contacts:member:detail"})
    @GetMapping("/detail/{id}")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizUserService.getOrgUserProfile(id));
    }

    @Operation(summary = "组织用户密码初始化", description = "组织用户密码初始化")
    @PutMapping("/pwd/init")
    public R<?> initFixPwd(@RequestBody UserPasswordDTO params) {
        bizUserService.orgPasswordInit(params);
        return ok();
    }

    @Operation(summary = "组织用户密码修改", description = "组织用户密码修改")
    @PutMapping("/pwd")
    public R<?> fixPwd(@RequestBody UserPasswordDTO params) {
        bizUserService.fixPassword(params);
        return ok();
    }
}
