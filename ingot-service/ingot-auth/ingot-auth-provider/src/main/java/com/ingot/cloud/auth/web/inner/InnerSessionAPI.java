package com.ingot.cloud.auth.web.inner;

import java.util.List;

import com.ingot.cloud.auth.api.constants.AuthInnerPaths;
import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.service.biz.BizSessionService;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.context.SecurityContextRevokeRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>会话执行面内部接口（仅内网调用），是跨服务查询与撤销会话的唯一入口。</p>
 *
 * <p>BFF 登出、账号域改密/锁定联动、安全中心强制下线都经
 * {@link com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService} 落到本接口，
 * 撤销动作统一委托 {@link SessionRevocationService}，保证 OAuth2 授权与会话主数据同时失效。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(AuthInnerPaths.SESSION)
@RequiredArgsConstructor
public class InnerSessionAPI implements RShortcuts {

    /**
     * 调用方未指定原因时的兜底口径：能到达本接口的撤销都是他人发起的被动下线。
     */
    private static final SessionRevokeReason DEFAULT_REASON = SessionRevokeReason.ADMIN_REVOKE;

    private final BizSessionService bizSessionService;
    private final SessionRevocationService sessionRevocationService;
    private final SecurityContextRevokeRepository securityContextRevokeRepository;

    @GetMapping(AuthInnerPaths.Session.PAGE)
    public R<InnerSessionPageVO> page(InnerSessionQueryDTO params) {
        return ok(bizSessionService.page(params));
    }

    /**
     * 查询单个会话；会话已过期或已撤销时返回成功且 data 为空。
     */
    @GetMapping(AuthInnerPaths.Session.SID)
    public R<InnerSessionVO> getBySid(@PathVariable(AuthInnerPaths.Session.VARIABLE_SID) String sid) {
        return ok(bizSessionService.getBySid(sid));
    }

    @GetMapping(AuthInnerPaths.Session.USER)
    public R<List<InnerSessionVO>> listByUser(InnerSessionQueryDTO params) {
        return ok(bizSessionService.listByUser(params));
    }

    /**
     * 按会话 ID 撤销。
     *
     * <p>调用方携带 Auth 侧登录 Cookie 时同步清理授权服务器自身的 SecurityContext，
     * 否则登出后仍能凭旧 Cookie 走 session 预授权重新签发 Token；未携带 Cookie 时该步为空操作。</p>
     */
    @DeleteMapping(AuthInnerPaths.Session.SID)
    public R<Boolean> revokeBySid(HttpServletRequest request,
                                  @PathVariable(AuthInnerPaths.Session.VARIABLE_SID) String sid,
                                  @RequestBody InnerSessionRevokeDTO params) {
        boolean revoked = sessionRevocationService.revokeBySid(
                sid, reasonOrDefault(params.getReason()), params.getActorId());
        securityContextRevokeRepository.revokeContext(request);
        return ok(revoked);
    }

    /**
     * 按用户撤销会话；{@code clientId} 为空表示该用户在当前租户下的全部 Client。
     */
    @DeleteMapping(AuthInnerPaths.Session.USER)
    public R<Integer> revokeByUser(@RequestBody InnerUserSessionRevokeDTO params) {
        return ok(sessionRevocationService.revokeByUser(params.getTenantId(), params.getClientId(),
                params.getUserId(), reasonOrDefault(params.getReason()), params.getActorId()));
    }

    /**
     * 租户级批量撤销，P1 预留路径。
     *
     * <p>返回 501 而非 404，以便将来实现时调用方无需变更路径；当前没有调用方依赖该能力。</p>
     */
    @DeleteMapping(AuthInnerPaths.Session.TENANT)
    public ResponseEntity<Void> revokeByTenant(
            @PathVariable(AuthInnerPaths.Session.VARIABLE_TENANT_ID) Long tenantId) {
        log.warn("[InnerSessionAPI] 租户级会话撤销尚未实现: tenantId={}", tenantId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private static SessionRevokeReason reasonOrDefault(SessionRevokeReason reason) {
        return reason == null ? DEFAULT_REASON : reason;
    }
}
