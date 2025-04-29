package com.ingot.cloud.pms.web.v1.admin;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.AllOrgUserFilterDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.vo.user.SimpleUserWithPhoneVO;
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
    @GetMapping("/searchByPhone")
    @Operation(summary = "根据手机号查询用户信息", description = "根据手机号查询用户信息")
    public R<?> searchByPhone(@RequestParam String phone) {
        return ok(CollUtil.emptyIfNull(sysUserService.list(
                        Wrappers.<SysUser>lambdaQuery()
                                .like(SysUser::getPhone, phone)))
                .stream()
                .map(item -> {
                    SimpleUserWithPhoneVO result = new SimpleUserWithPhoneVO();
                    BeanUtil.copyProperties(item, result);
                    return result;
                }).toList());
    }

    @HasAnyAuthority({"basic:user:r", "basic:user:w"})
    @GetMapping("/page")
    @Operation(summary = "用户分页接口", description = "用户分页接口")
    public R<?> page(Page<SysUser> page, AllOrgUserFilterDTO condition) {
        return ok(sysUserService.allOrgUserPage(page, condition));
    }

    @HasAnyAuthority({"basic:user:w"})
    @PostMapping
    @Operation(summary = "创建用户", description = "创建系统用户")
    public R<?> create(@Validated(Group.Create.class) @RequestBody UserDTO params) {
        return ok(bizUserService.createUser(params));
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping
    @Operation(summary = "更新用户", description = "更新系统用户")
    public R<?> update(@Validated(Group.Update.class) @RequestBody UserDTO params) {
        bizUserService.updateUser(params);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除系统用户")
    public R<?> removeById(@PathVariable Long id) {
        bizUserService.deleteUser(id);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w", "basic:user:r"})
    @GetMapping("/orgInfo/{userId}")
    @Operation(summary = "用户组织信息", description = "用户组织信息")
    public R<?> orgInfo(@PathVariable Long userId) {
        return ok(bizUserService.userOrgInfo(userId));
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping("/org")
    @Operation(summary = "用户组织信息编辑", description = "用户组织信息编辑")
    public R<?> userOrgEdit(@RequestBody UserOrgEditDTO params) {
        bizUserService.userOrgEdit(params);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping("/org/leave")
    @Operation(summary = "用户离开组织", description = "用户离开组织")
    public R<?> userOrgLeave(@RequestBody UserOrgEditDTO params) {
        bizUserService.userOrgLeave(params);
        return ok();
    }

    @HasAnyAuthority({"basic:user:w"})
    @PutMapping("/resetPwd/{userId}")
    @Operation(summary = "重置密码", description = "重置密码")
    public R<?> resetPwd(@PathVariable Long userId) {
        return ok(bizUserService.resetPwd(userId));
    }

    @HasAnyAuthority({"basic:user:w", "basic:user:r"})
    @GetMapping("/profile/{id}")
    @Operation(summary = "用户简介信息", description = "用户简介信息")
    public R<?> userProfile(@PathVariable Long id) {
        return ok(bizUserService.getUserProfile(id));
    }

    @PutMapping("/edit")
    @Operation(summary = "更新用户基本信息", description = "更新用户基本信息")
    public R<?> updateUserBaseInfo(@RequestBody UserBaseInfoDTO params) {
        long userId = SecurityAuthContext.getUser().getId();
        bizUserService.updateUserBaseInfo(userId, params);
        return ok();
    }
}
