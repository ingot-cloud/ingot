package com.ingot.cloud.security.web.platform.security;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ingot.cloud.security.api.model.dto.session.PlatformSessionQueryDTO;
import com.ingot.cloud.security.api.model.dto.session.PlatformUserSessionRevokeDTO;
import com.ingot.cloud.security.api.model.vo.session.PlatformSessionVO;
import com.ingot.cloud.security.service.session.SessionAdminService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>安全中心在线会话管理面，是管理员查询会话与强制下线的唯一入口。</p>
 *
 * <p>原 Auth 的 {@code /auth/token/**} 已随 {@code TokenEndpoint} 删除，会话执行面收口到
 * Auth Inner RPC，管理前端一律改走本接口。撤销原因固定为管理员撤销，操作者取当前登录管理员，
 * 不接受前端传入，避免安全事件的操作者与原因被伪造。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 经网关访问时前缀为 {@code /security}，即 {@code /security/platform/security/sessions}。
 * 下线接口用 {@code DELETE} + query 参数而非请求体，规避部分网关与浏览器丢弃 DELETE body 的问题。
 */
@RestController
@RequestMapping("/platform/security/sessions")
@RequiredArgsConstructor
@Tag(name = "安全中心-在线会话")
public class PlatformSessionAPI implements RShortcuts {

    private final SessionAdminService sessionAdminService;

    @GetMapping
    @Operation(summary = "分页查询在线会话")
    @AdminOrHasAnyAuthority({"platform:security:session:query"})
    public R<IPage<PlatformSessionVO>> page(PlatformSessionQueryDTO params) {
        return ok(sessionAdminService.page(params));
    }

    @GetMapping("/{sid}")
    @Operation(summary = "查询会话详情")
    @AdminOrHasAnyAuthority({"platform:security:session:query"})
    public R<PlatformSessionVO> getBySid(@PathVariable String sid) {
        return ok(sessionAdminService.getBySid(sid));
    }

    @DeleteMapping("/{sid}")
    @Operation(summary = "强制下线指定会话")
    @AdminOrHasAnyAuthority({"platform:security:session:revoke"})
    public R<Boolean> revokeBySid(@PathVariable String sid) {
        return ok(sessionAdminService.revokeBySid(sid, currentUserId()));
    }

    @DeleteMapping("/user")
    @Operation(summary = "强制下线用户全部会话")
    @AdminOrHasAnyAuthority({"platform:security:session:revoke"})
    public R<Integer> revokeByUser(PlatformUserSessionRevokeDTO params) {
        return ok(sessionAdminService.revokeByUser(params, currentUserId()));
    }

    /**
     * 当前登录管理员 ID，写入安全事件的操作者。
     */
    private Long currentUserId() {
        InUser user = SecurityAuthContext.getUser();
        return user == null ? null : user.getId();
    }
}
