package com.ingot.cloud.member.web.v1.platform;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.dto.user.MemberUserBaseInfoDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
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
    private final BizUserService bizUserService;

    @Operation(summary = "会员用户分页", description = "会员用户分页查询")
    @AdminOrHasAnyAuthority({"platform:member:user:query"})
    @GetMapping("/page")
    public R<?> page(Page<MemberUser> page, MemberUserDTO condition) {
        return ok(bizUserService.conditionPage(page, condition));
    }

    @Operation(summary = "会员用户详情", description = "根据ID获取会员用户详情")
    @AdminOrHasAnyAuthority({"platform:member:user:detail"})
    @GetMapping("/detail/{id}")
    public R<?> detail(@PathVariable Long id) {
        return ok(bizUserService.getUserProfile(id));
    }

    @Operation(summary = "创建会员用户", description = "创建新的会员用户")
    @AdminOrHasAnyAuthority({"platform:member:user:create"})
    @PostMapping
    public R<?> create(@RequestBody MemberUserDTO params) {
        return ok(bizUserService.createUser(params));
    }

    @Operation(summary = "更新会员用户", description = "更新会员用户信息")
    @AdminOrHasAnyAuthority({"platform:member:user:update"})
    @PutMapping
    public R<?> update(@RequestBody MemberUserDTO params) {
        bizUserService.updateUser(params);
        return ok();
    }

    @Operation(summary = "删除会员用户", description = "根据ID删除会员用户")
    @AdminOrHasAnyAuthority({"platform:member:user:delete"})
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        bizUserService.deleteUser(id);
        return ok();
    }

    @Operation(summary = "更新用户基本信息", description = "更新会员用户基本信息")
    @AdminOrHasAnyAuthority({"platform:member:user:update"})
    @PutMapping("/{id}/base-info")
    public R<?> updateBaseInfo(@PathVariable Long id, @RequestBody MemberUserBaseInfoDTO params) {
        bizUserService.updateUserBaseInfo(id, params);
        return ok();
    }

    @Operation(summary = "重置用户密码", description = "重置会员用户密码")
    @AdminOrHasAnyAuthority({"platform:member:user:reset-pwd"})
    @PutMapping("/{id}/reset-password")
    public R<ResetPwdVO> resetPassword(@PathVariable Long id) {
        return ok(bizUserService.resetPwd(id));
    }

    @Operation(summary = "获取用户角色", description = "获取会员用户的角色ID列表")
    @AdminOrHasAnyAuthority({"platform:member:user:roles:query"})
    @GetMapping("/{id}/roles")
    public R<List<MemberRole>> getUserRoles(@PathVariable Long id) {
        return ok(bizUserService.getUserRoles(id));
    }

    @Operation(summary = "设置用户角色", description = "设置会员用户的角色")
    @AdminOrHasAnyAuthority({"platform:member:user:roles:set"})
    @PutMapping("/{id}/roles")
    public R<?> setUserRoles(@PathVariable Long id,
                             @RequestBody SetDTO<Long, Long> params) {
        params.setId(id);
        bizUserService.setUserRoles(id, params.getSetIds());
        return ok();
    }
}

