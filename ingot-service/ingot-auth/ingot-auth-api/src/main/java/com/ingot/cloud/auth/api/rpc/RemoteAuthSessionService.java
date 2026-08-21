package com.ingot.cloud.auth.api.rpc;

import java.util.List;

import com.ingot.cloud.auth.api.constants.AuthInnerPaths;
import com.ingot.cloud.auth.api.model.dto.InnerSessionQueryDTO;
import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionPageVO;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * <p>Auth 会话执行面的 Feign 契约，是跨服务查询与撤销会话的唯一入口。</p>
 *
 * <p>会话主数据只存在于 Auth 侧 Redis，任何服务都不得自行删除会话键或 OAuth2 授权，
 * 否则会留下「Access Token 失效但 Refresh Token 仍可换新」的半撤销状态。
 * BFF 登出、账号域改密/锁定联动、安全中心强制下线统一走本接口。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.commons.model.security.SessionRevokeReason
 * @apiNote 调用不经网关，由 Nacos 按 {@link ServiceNameConstants#AUTH_SERVICE} 直连，
 * 服务端以 {@code @Permit(INNER)} 限制仅内网可达。
 */
@FeignClient(contextId = "RemoteAuthSessionService", value = ServiceNameConstants.AUTH_SERVICE)
public interface RemoteAuthSessionService {

    /**
     * 分页查询在线会话。
     *
     * @param params {@code tenantId} 与 {@code clientId} 必填，{@code userId} / {@code ipAddress} 可选
     */
    @GetMapping(AuthInnerPaths.SESSION + AuthInnerPaths.Session.PAGE)
    R<InnerSessionPageVO> page(@SpringQueryMap InnerSessionQueryDTO params);

    /**
     * 查询单个会话；会话不存在时返回成功且 data 为空，不作为异常处理。
     */
    @GetMapping(AuthInnerPaths.SESSION + AuthInnerPaths.Session.SID)
    R<InnerSessionVO> getBySid(@PathVariable(AuthInnerPaths.Session.VARIABLE_SID) String sid);

    /**
     * 查询用户名下全部在线会话。
     *
     * @param params {@code tenantId} 与 {@code userId} 必填，{@code clientId} 为空表示全部 Client
     */
    @GetMapping(AuthInnerPaths.SESSION + AuthInnerPaths.Session.USER)
    R<List<InnerSessionVO>> listByUser(@SpringQueryMap InnerSessionQueryDTO params);

    /**
     * 按会话 ID 撤销。
     *
     * @param cookie 可选，携带 Auth 侧登录 Cookie 时同步清理授权服务器自身的登录态，
     *               避免登出后仍能凭旧 Cookie 走 session 预授权重新签发 Token
     * @return {@code true} 表示本次调用确实撤销了一个存活会话
     */
    @DeleteMapping(AuthInnerPaths.SESSION + AuthInnerPaths.Session.SID)
    R<Boolean> revokeBySid(@RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
                           @PathVariable(AuthInnerPaths.Session.VARIABLE_SID) String sid,
                           @RequestBody InnerSessionRevokeDTO params);

    /**
     * 按用户撤销会话。
     *
     * @return 实际撤销的会话数
     */
    @DeleteMapping(AuthInnerPaths.SESSION + AuthInnerPaths.Session.USER)
    R<Integer> revokeByUser(@RequestBody InnerUserSessionRevokeDTO params);
}
