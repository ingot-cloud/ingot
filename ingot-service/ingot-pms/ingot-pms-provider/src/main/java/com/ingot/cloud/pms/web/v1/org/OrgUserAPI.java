package com.ingot.cloud.pms.web.v1.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantContextHolder;
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
@RestController
@RequestMapping(value = "/v1/org/user")
@RequiredArgsConstructor
public class OrgUserAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final BizUserService bizUserService;

    @GetMapping
    public R<?> user() {
        return ok(sysUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @AdminOrHasAnyAuthority({"contacts:member:w", "contacts:member:r"})
    @GetMapping("/page")
    public R<?> page(Page<SysUser> page, UserQueryDTO condition) {
        Long tenantId = TenantContextHolder.get();
        return ok(sysUserService.conditionPage(page, condition, tenantId));
    }

    @AdminOrHasAnyAuthority({"contacts:member:w"})
    @PostMapping
    public R<?> create(@RequestBody OrgUserDTO params) {
        bizUserService.orgCreateUser(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"contacts:member:w"})
    @PutMapping
    public R<?> update(@RequestBody OrgUserDTO params) {
        bizUserService.orgUpdateUser(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"contacts:member:w"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizUserService.orgDeleteUser(id);
        return ok();
    }

    @AdminOrHasAnyAuthority({"contacts:member:w", "contacts:member:r"})
    @GetMapping("/profile/{id}")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizUserService.getOrgUserProfile(id));
    }

    @PutMapping("/edit")
    public R<?> updateUserBaseInfo(@RequestBody UserBaseInfoDTO params) {
        long userId = SecurityAuthContext.getUser().getId();
        bizUserService.updateUserBaseInfo(userId, params);
        return ok();
    }

    @PutMapping("/initFixPwd")
    public R<?> initFixPwd(@RequestBody UserPasswordDTO params) {
        bizUserService.orgPasswordInit(params);
        return ok();
    }

    @PutMapping("/fixPwd")
    public R<?> fixPwd(@RequestBody UserPasswordDTO params) {
        bizUserService.fixPassword(params);
        return ok();
    }
}
