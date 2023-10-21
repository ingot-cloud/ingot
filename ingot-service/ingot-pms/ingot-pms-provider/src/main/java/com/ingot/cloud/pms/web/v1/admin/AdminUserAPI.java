package com.ingot.cloud.pms.web.v1.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.AllOrgUserFilterDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/11.</p>
 * <p>Time         : 6:48 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/v1/admin/user")
@RequiredArgsConstructor
public class AdminUserAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final BizUserService bizUserService;

    @GetMapping
    public R<?> user() {
        return ok(sysUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.read', 'basic.user.write')")
    @GetMapping("/page")
    public R<?> page(Page<SysUser> page, AllOrgUserFilterDTO condition) {
        return ok(sysUserService.allOrgUserPage(page, condition));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.write')")
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody UserDTO params) {
        return ok(bizUserService.createUser(params));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.write')")
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody UserDTO params) {
        bizUserService.updateUser(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.write')")
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizUserService.deleteUser(id);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.read')")
    @GetMapping("/orgInfo/{id}")
    public R<?> orgInfo(@PathVariable Long id) {
        return ok(bizUserService.userOrgInfo(id));
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.write')")
    @PutMapping("/org")
    public R<?> userOrgEdit(@RequestBody UserOrgEditDTO params) {
        bizUserService.userOrgEdit(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.write')")
    @PutMapping("/org/leave")
    public R<?> userOrgLeave(@RequestBody UserOrgEditDTO params) {
        bizUserService.userOrgLeave(params);
        return ok();
    }

    @PreAuthorize("@ingot.hasAnyAuthority('basic.user.read', 'basic.user.write')")
    @GetMapping("/profile/{id}")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizUserService.getUserProfile(id));
    }

    @PutMapping("/edit")
    public R<?> updateUserBaseInfo(@RequestBody UserBaseInfoDTO params) {
        long userId = SecurityAuthContext.getUser().getId();
        bizUserService.updateUserBaseInfo(userId, params);
        return ok();
    }
}
