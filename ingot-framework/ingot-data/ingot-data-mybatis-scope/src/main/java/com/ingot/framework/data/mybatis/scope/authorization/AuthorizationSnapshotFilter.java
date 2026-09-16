package com.ingot.framework.data.mybatis.scope.authorization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.data.mybatis.scope.error.AuthorizationSnapshotException;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>在 Bearer 鉴权之后把授权快照的具体权限码合并进当前 Authentication，并绑定请求期快照。</p>
 *
 * <p>内部接口与未认证请求跳过，避免登录补全与快照接口递归调用。过滤器位于 DispatcherServlet
 * 之前，快照不可用时直接写 503，不能依赖 {@code @RestControllerAdvice}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AuthorizationSnapshotFilter extends OncePerRequestFilter {

    private final AuthorizationSnapshotAccess snapshotAccess;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        InUser user = SecurityAuthContext.getUser();
        // IAM 成员上下文由新引擎求值，禁止用旧账号/租户快照扩充权限。
        if (user == null || user.getAuthorizationContext() != null
                || user.getId() == null || user.getTenantId() == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthorizationSnapshotDTO snapshot = snapshotAccess.require(user.getTenantId(), user.getId());
            AuthorizationSnapshotHolder.set(snapshot);
            mergeAuthorities(snapshot);
            filterChain.doFilter(request, response);
        } catch (AuthorizationSnapshotException ex) {
            writeUnavailable(response, ex);
        } finally {
            AuthorizationSnapshotHolder.clear();
        }
    }

    private static boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains(AuthorizationSnapshotConstants.INNER_PATH_PREFIX);
    }

    private void writeUnavailable(HttpServletResponse response, AuthorizationSnapshotException ex) throws IOException {
        log.warn("[AuthorizationSnapshot] snapshot unavailable", ex);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        R<?> body = R.error(ex.getCode(), ex.getLocalizedMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.flushBuffer();
    }

    private static void mergeAuthorities(AuthorizationSnapshotDTO snapshot) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof InJwtAuthenticationToken jwtAuthentication)) {
            return;
        }
        if (!(jwtAuthentication.getPrincipal() instanceof InUser current)) {
            return;
        }
        Collection<GrantedAuthority> original = jwtAuthentication.getAuthorities();
        List<GrantedAuthority> merged = new ArrayList<>(original);
        Set<String> existing = new LinkedHashSet<>();
        for (GrantedAuthority authority : original) {
            existing.add(authority.getAuthority());
        }
        for (String code : CollUtil.emptyIfNull(snapshot.getPermissionCodes())) {
            if (StrUtil.isBlank(code)) {
                continue;
            }
            String granted = InJwtAuthenticationConverter.AUTHORITY_PREFIX + code;
            if (existing.add(granted)) {
                merged.add(new SimpleGrantedAuthority(granted));
            }
        }
        Jwt jwt = jwtAuthentication.getToken();
        InUser refreshed = InUser.stateless(
                current.getId(),
                current.getTenantId(),
                current.getClientId(),
                current.getTokenAuthType(),
                current.getUserType(),
                current.getUsername(),
                merged,
                current.getDeptIds(),
                current.getTenantDeptIds());
        InJwtAuthenticationToken next = new InJwtAuthenticationToken(
                jwt, refreshed, merged, jwtAuthentication.getName());
        next.setDetails(jwtAuthentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(next);
    }
}
