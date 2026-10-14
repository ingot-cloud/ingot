package com.ingot.cloud.iam.authorization.read;

import java.time.Instant;
import java.util.function.Supplier;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator.AuthorizationView;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>只读 MVC 请求的身份绑定结果；普通缓存与实时求值分开，写事务和其他调用不复用。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IamReadRequestContext {
    private static final String ATTRIBUTE = IamReadRequestContext.class.getName();
    private final Authentication authentication;
    private final AuthorizationContext actor;
    private ActiveIdentity identity;
    private Boolean passwordRequired;
    private AuthorizationView ordinary;
    private AuthorizationView fresh;

    private IamReadRequestContext(Authentication authentication, AuthorizationContext actor) {
        this.authentication = authentication;
        this.actor = actor;
    }

    /** 在可信同步 GET 的方法鉴权之前启用；不执行或代替认证。 */
    static void begin(HttpServletRequest request) {
        clear(request);
        var authentication = SecurityAuthContext.getAuthentication();
        if (eligible(request) && authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof InUser user && user.getAuthorizationContext() != null) {
            request.setAttribute(ATTRIBUTE, new IamReadRequestContext(authentication, user.getAuthorizationContext()));
        }
    }

    /** 请求结束或异步移交时丢弃结果，不传播至工作线程。 */
    static void clear(HttpServletRequest request) {
        request.removeAttribute(ATTRIBUTE);
    }

    /** 返回本请求的有效身份；不匹配当前完整认证上下文时直接重新查询。 */
    public static ActiveIdentity identity(AuthorizationContext actor, Supplier<ActiveIdentity> loader) {
        var context = current(actor);
        if (context == null) return loader.get();
        if (context.identity == null) context.identity = loader.get();
        return context.identity;
    }

    /** 复用本次读取的改密状态；异常不写入结果。 */
    public static boolean passwordRequired(AuthorizationContext actor, Supplier<Boolean> loader) {
        var context = current(actor);
        if (context == null) return loader.get();
        if (context.passwordRequired == null) context.passwordRequired = loader.get();
        return context.passwordRequired;
    }

    /** 同来源复用未到期视图；实时来源永不由普通授权缓存满足。 */
    public static AuthorizationView view(AuthorizationContext actor, boolean fresh, Supplier<AuthorizationView> loader) {
        var context = current(actor);
        if (context == null) return loader.get();
        var view = fresh ? context.fresh : context.ordinary;
        if (view == null || view.expired(Instant.now())) {
            view = loader.get();
            if (fresh) context.fresh = view;
            else context.ordinary = view;
        }
        return view;
    }

    private static IamReadRequestContext current(AuthorizationContext actor) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return null;
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        var request = attributes.getRequest();
        if (!eligible(request) || request.isAsyncStarted()
                || !(request.getAttribute(ATTRIBUTE) instanceof IamReadRequestContext context)) return null;
        var authentication = SecurityAuthContext.getAuthentication();
        if (authentication != context.authentication || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof InUser user)
                || !context.actor.equals(user.getAuthorizationContext()) || !context.actor.equals(actor)) return null;
        return context;
    }

    private static boolean eligible(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) && request.getDispatcherType() == DispatcherType.REQUEST;
    }
}
