package com.ingot.framework.authorization;

import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import feign.FeignException;

/**
 * <p>
 * v2 远程授权，缓存隔离完整身份与资源；写操作不接受热缓存放行。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RemoteAuthorizationClient implements AuthorizationClient {

    private final RemoteIamAuthorizationService remote;

    private final ResourceRegistry registry;

    private final LayeredCache<String, AuthorizationDecision> cache;

    private final ObjectMapper mapper;

    /**
     * 装配客户端。
     * @param remote RPC
     * @param registry 业务接入资源
     * @param cache 缓存
     * @param mapper JSON
     */
    public RemoteAuthorizationClient(RemoteIamAuthorizationService remote, ResourceRegistry registry,
            LayeredCache<String, AuthorizationDecision> cache, ObjectMapper mapper) {
        this.remote = remote;
        this.registry = registry;
        this.cache = cache;
        this.mapper = mapper;
    }

    @Override
    public AuthorizationDecision evaluate(AuthorizationRequest request) { return evaluate(request, false); }

    @Override
    public AuthorizationDecision preview(AuthorizationRequest request) { return evaluate(request, true); }

    private AuthorizationDecision evaluate(AuthorizationRequest request, boolean preview) {
        AuthorizationContext actor = current();
        if (actor.domain() != request.resource().domain())
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        var selected = registry.require(request.resource())
            .descriptor()
            .actions()
            .stream()
            .filter(action -> request.actionCodes().contains(action.code()))
            .toList();
        if (selected.size() != request.actionCodes().stream().distinct().count()) {
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        }
        try {
            if (!preview && selected.stream().anyMatch(action -> action.mode() == ExecutionMode.MUTATING)) {
                return load(remote, request, actor);
            }
            String key = mapper.writeValueAsString(new CacheKey(actor, request, preview));
            AuthorizationDecision value = cache.get(key);
            if (expired(value)) {
                cache.evict(key);
                value = cache.get(key);
            }
            validate(value, request, actor);
            return value;
        }
        catch (BizException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
    }

    /**
     * 加载远端结论，保持明确拒绝与基础设施故障的区别。
     * @param remote 内部 RPC
     * @param request 声明资源
     * @param actor 可信身份
     * @return 有效结论
     */
    public static AuthorizationDecision load(RemoteIamAuthorizationService remote, AuthorizationRequest request,
            AuthorizationContext actor) {
        return load(remote, request, actor, false);
    }

    /** 服务器选定预览端点，缓存键与执行结论隔离。 */
    public static AuthorizationDecision load(RemoteIamAuthorizationService remote, AuthorizationRequest request,
            AuthorizationContext actor, boolean preview) {
        try {
            var response = preview ? remote.preview(request) : remote.evaluate(request);
            if (response == null)
                throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            if (!response.isSuccess()) {
                var reason = java.util.Arrays.stream(IamReasonCode.values())
                    .filter(code -> code.getCode().equals(response.getCode()))
                    .findFirst()
                    .orElse(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
                throw new SdkAuthorizationException(reason);
            }
            validate(response.getData(), request, actor);
            return response.getData();
        }
        catch (FeignException exception) {
            throw new SdkAuthorizationException(switch (exception.status()) {
                case 401 -> IamReasonCode.IDENTITY_INVALID;
                case 403 -> IamReasonCode.ACTION_DENIED;
                case 404 -> IamReasonCode.OBJECT_NOT_FOUND;
                case 400 -> IamReasonCode.INVALID_ARGUMENT;
                default -> IamReasonCode.AUTHORIZATION_UNAVAILABLE;
            });
        }
    }

    /**
     * 恢复当前认证中的成员身份。
     * @return 当前可信上下文
     */
    public static AuthorizationContext current() {
        var user = SecurityAuthContext.getUser();
        if (user == null || user.getAuthorizationContext() == null)
            throw new SdkAuthorizationException(IamReasonCode.IDENTITY_INVALID);
        return user.getAuthorizationContext();
    }

    /**
     * 清除本节点与共享热缓存。
     */
    public void evictAll() {
        cache.evictAll();
    }

    private static void validate(AuthorizationDecision value, AuthorizationRequest request,
            AuthorizationContext actor) {
        if (expired(value))
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        if (!Objects.equals(actor, value.context()) || !Objects.equals(request.resource(), value.resource())) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
        request.actionCodes().forEach(code -> FieldPolicyProcessor.forAction(value, code));
    }

    private static boolean expired(AuthorizationDecision value) {
        return value == null || value.expiresAt() == null || !Instant.now().isBefore(value.expiresAt());
    }

    /**
     * <p>
     * 缓存键不允许在其它身份线程中重新加载。
     * </p>
     *
     * @param actor 可信身份
     * @param request 精确请求
     * @param preview 交互预览目的
     * @author jy
     * @since 1.0.0
     */
    public record CacheKey(AuthorizationContext actor, AuthorizationRequest request, boolean preview) {
        /** 已有读执行调用使用执行目的。 */
        public CacheKey(AuthorizationContext actor, AuthorizationRequest request) { this(actor, request, false); }
    }

}
