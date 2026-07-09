package com.ingot.cloud.bff.web;

import java.util.Map;

import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.cloud.bff.model.dto.BffTenantSelectDTO;
import com.ingot.cloud.bff.service.BffAuthService;
import com.ingot.cloud.bff.service.BffSessionService;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.annotation.InDecrypt;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>BFF 认证 REST API，为内部前端系统提供简化的登录/登出接口</p>
 *
 * <p>前端不接触任何 OAuth2 参数或 JWT，仅通过 HttpOnly Session Cookie 维持会话。
 * 登录流程：{@code POST /login} → {@code POST /tenant/select} → 完成。</p>
 *
 * <h3>前端调用示例：</h3>
 * <pre>{@code
 * // 1. 登录，拿到可选租户列表
 * POST /bff/auth/login  { "username": "admin", "password": "xxx" }
 * // 响应 Set-Cookie: IN_SESSION=xxx; HttpOnly; SameSite=Lax
 *
 * // 2. 选租户，完成授权
 * POST /bff/auth/tenant/select  { "tenantId": "1" }
 *
 * // 3. 后续业务请求自动携带 Cookie，网关注入 JWT
 * GET /bff/xxx  (Cookie 自动带上)
 * }</pre>
 *
 * @author jy
 * @see BffAuthService
 * @see BffSessionService
 * @since 1.0.0
 */
@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
public class BffAuthAPI implements RShortcuts {
    private final BffAuthService authService;
    private final BffSessionService sessionService;

    /**
     * 登录：提交账号密码，返回可选租户列表。
     * 前端只传 username/password/vcCode，不传任何 OAuth2 参数。
     *
     * @param dto      登录参数
     * @param request  当前请求
     * @param response 当前响应
     * @return 可选租户列表
     */
    @Permit
    @InCryptoHybridContext
    @InDecrypt
    @PostMapping("/login")
    public R<?> login(@RequestBody BffLoginDTO dto,
                      HttpServletRequest request,
                      HttpServletResponse response) {
        return authService.login(dto, request, response);
    }

    /**
     * 选择租户：完成授权码流程，建立 BFF session。
     * 前端只传 tenantId，成功后 Cookie 中的 session 即可用于后续所有业务请求。
     *
     * @param dto      租户选择参数
     * @param request  当前请求
     * @param response 当前响应
     * @return 成功/失败
     */
    @Permit
    @PostMapping("/tenant/select")
    public R<?> selectTenant(@RequestBody BffTenantSelectDTO dto,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        return authService.selectTenant(dto.getTenantId(), dto.getRedirectUri(), request, response);
    }

    /**
     * 登出：撤销 Token + 清除 BFF session。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @return 成功
     */
    @Permit
    @DeleteMapping("/logout")
    public R<?> logout(HttpServletRequest request,
                       HttpServletResponse response) {
        return authService.logout(request, response);
    }

    /**
     * 获取当前登录用户的基础信息。
     *
     * @param request 当前请求
     * @return 用户信息（tenantId、userId、clientId）
     */
    @GetMapping("/me")
    public R<?> me(HttpServletRequest request) {
        BffSession session = sessionService.getSession(request);
        if (session == null) {
            return R.error("S0401", "not authenticated");
        }
        return ok(Map.of(
                "tenantId", session.getTenantId() != null ? session.getTenantId() : "",
                "userId", session.getUserId() != null ? session.getUserId() : 0,
                "clientId", session.getClientId() != null ? session.getClientId() : ""
        ));
    }
}
