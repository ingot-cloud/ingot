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
import com.ingot.framework.security.access.HasAnyAuthority;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/11.</p>
 * <p>Time         : 6:48 下午.</p>
 */
@Slf4j
@Tag(description = "user", name = "系统用户管理模块")
@RestController
@RequestMapping(value = "/v1/admin/user")
@RequiredArgsConstructor
public class AdminUserAPI implements RShortcuts {
    private final SysUserService sysUserService;
    private final BizUserService bizUserService;

    @GetMapping
    @Operation(summary = "获取用户信息", description = "根据当前Token获取用户信息")
    public R<?> user() {
        return ok(sysUserService.getUserInfo(SecurityAuthContext.getUser()));
    }

    @HasAnyAuthority({"basic:user:r", "basic:user:w"})
    @GetMapping("/page")
    public R<?> page(Page<SysUser> page, AllOrgUserFilterDTO condition) {
        return ok(sysUserService.allOrgUserPage(page, condition));
    }

    @HasAnyAuthority({"basic:user:w"})
    @PostMapping
    public R<?> create(@Validated(Group.Create.class) @RequestBody UserDTO params) {
        return ok(bizUserService.createUser(params));
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping
    public R<?> update(@Validated(Group.Update.class) @RequestBody UserDTO params) {
        bizUserService.updateUser(params);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w"})
    @DeleteMapping("/{id}")
    public R<?> removeById(@PathVariable Long id) {
        bizUserService.deleteUser(id);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w", "basic:user:r"})
    @GetMapping("/orgInfo/{userId}")
    public R<?> orgInfo(@PathVariable Long userId) {
        return ok(bizUserService.userOrgInfo(userId));
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping("/org")
    public R<?> userOrgEdit(@RequestBody UserOrgEditDTO params) {
        bizUserService.userOrgEdit(params);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping("/org/leave")
    public R<?> userOrgLeave(@RequestBody UserOrgEditDTO params) {
        bizUserService.userOrgLeave(params);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping("/resetPwd/{userId}")
    public R<?> resetPwd(@PathVariable Long userId) {
        return ok(bizUserService.resetPwd(userId));
    }

    @HasAnyAuthority({"basic:user:w", "basic:user:r"})
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
