package com.ingot.cloud.member.web.v1.platform;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.dto.user.MemberAccountLockDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserBaseInfoDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.cloud.member.authorization.MemberGlobalAccess;
import com.ingot.framework.commons.model.iam.MemberAdminResources;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : MemberUserAPI.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Slf4j
@Tag(name = "会员用户模块", description = "MemberUser")
@RestController
@RequestMapping(value = "/v1/platform/member/user")
@RequiredArgsConstructor
public class MemberUserAPI implements RShortcuts {
    private final MemberGlobalAccess access;
    private final BizUserService bizUserService;

    /** 会员用户分页，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "会员用户分页", description = "会员用户分页查询")
    @GetMapping("/page")
    public R<?> page(Page<MemberUser> page, MemberUserDTO condition) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_QUERY);
        return ok(bizUserService.conditionPage(page, condition));
    }

    /** 会员用户详情，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "会员用户详情", description = "根据ID获取会员用户详情")
    @GetMapping("/detail/{id}")
    public R<?> detail(@PathVariable Long id) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_DETAIL);
        return ok(bizUserService.getUserProfile(id));
    }

    /** 创建会员用户，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "创建会员用户", description = "创建新的会员用户")
    @PostMapping
    public R<?> create(@RequestBody MemberUserDTO params) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_CREATE);
        return ok(bizUserService.createUser(params));
    }

    /** 更新会员用户，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "更新会员用户", description = "更新会员用户信息")
    @PutMapping
    public R<?> update(@RequestBody MemberUserDTO params) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_UPDATE);
        bizUserService.updateUser(params);
        return ok();
    }

    /** 删除会员用户，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "删除会员用户", description = "根据ID删除会员用户")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_DELETE);
        bizUserService.deleteUser(id);
        return ok();
    }

    /** 更新用户基本信息，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "更新用户基本信息", description = "更新会员用户基本信息")
    @PutMapping("/{id}/base-info")
    public R<?> updateBaseInfo(@PathVariable Long id, @RequestBody MemberUserBaseInfoDTO params) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_UPDATE);
        bizUserService.updateUserBaseInfo(id, params);
        return ok();
    }

    /** 重置用户密码，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "重置用户密码", description = "重置会员用户密码")
    @PutMapping("/{id}/reset-password")
    public R<ResetPwdVO> resetPassword(@PathVariable Long id) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_RESET_PWD);
        return ok(bizUserService.resetPwd(id));
    }

    /** 获取用户角色，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "获取用户角色", description = "获取会员用户的角色ID列表")
    @GetMapping("/{id}/roles")
    public R<List<MemberRole>> getUserRoles(@PathVariable Long id) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_ROLES_QUERY);
        return ok(bizUserService.getUserRoles(id));
    }

    /** 设置用户角色，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "设置用户角色", description = "设置会员用户的角色")
    @PutMapping("/{id}/roles")
    public R<?> setUserRoles(@PathVariable Long id,
                             @RequestBody SetDTO<Long, Long> params) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_ROLES_SET);
        params.setId(id);
        bizUserService.setUserRoles(id, params.getSetIds());
        return ok();
    }

    /** 启用账号，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "启用账号", description = "启用指定会员用户账号")
    @PutMapping("/{id}/enable")
    public R<?> enableAccount(@PathVariable Long id,
                              @RequestParam(required = false) String reason) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_UPDATE);
        bizUserService.enableAccount(id, reason);
        return ok();
    }

    /** 禁用账号，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "禁用账号", description = "禁用指定会员用户账号")
    @PutMapping("/{id}/disable")
    public R<?> disableAccount(@PathVariable Long id,
                               @RequestParam(required = false) String reason) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_UPDATE);
        bizUserService.disableAccount(id, reason);
        return ok();
    }

    /** 锁定账号，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "锁定账号", description = "手动锁定指定会员用户账号，可指定锁定原因和到期时间")
    @PutMapping("/{id}/lock")
    public R<?> lockAccount(@PathVariable Long id,
                            @RequestBody MemberAccountLockDTO params) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_LOCK);
        bizUserService.lockAccount(id, params);
        return ok();
    }

    /** 解锁账号，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "解锁账号", description = "手动解锁指定会员用户账号")
    @PutMapping("/{id}/unlock")
    public R<?> unlockAccount(@PathVariable Long id,
                              @RequestBody MemberAccountLockDTO params) {
        access.require(MemberAdminResources.USER, MemberAdminResources.USER_LOCK);
        bizUserService.unlockAccount(id, params.getReasonDetail());
        return ok();
    }
}

