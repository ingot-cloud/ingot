package com.ingot.framework.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.ActionDecision;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ObjectQueryPurpose;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectInvocation;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * 验证远程 SDK 使用当前身份、失效和绝对截止，写操作不借用热缓存。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class RemoteAuthorizationTest {

    private static final ResourceKey KEY = new ResourceKey(AuthorizationDomain.PLATFORM, "iam-ops", "incident");

    private static final String READ = "iam-ops:incident:read", WRITE = "iam-ops:incident:update";

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roleFieldJsonTransportPreservesExactActionAndTargetResults() throws Exception {
        var actor = authenticate("1", "101");
        var full = new com.ingot.framework.commons.model.iam.FieldAccess(
                com.ingot.framework.commons.model.iam.FieldVisibility.FULL, true);
        var masked = new com.ingot.framework.commons.model.iam.FieldAccess(
                com.ingot.framework.commons.model.iam.FieldVisibility.MASKED, false);
        var hidden = new com.ingot.framework.commons.model.iam.FieldAccess(
                com.ingot.framework.commons.model.iam.FieldVisibility.HIDDEN, false);
        var all = new ScopeCondition(true, List.of(), null, List.of());
        var narrow = new ScopeCondition(false, List.of("A"), null, List.of());
        var read = new FieldPolicyDecision(Map.of("phone", hidden), Map.of("phone", full), List.of(
                new com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule("phone", List.of(narrow), full),
                new com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule("phone", List.of(all), masked)),
                Map.of(), Map.of(), com.ingot.framework.commons.model.iam.FieldMergeMode.GRANTS);
        var write = new FieldPolicyDecision(Map.of("phone", hidden), Map.of("phone", full),
                List.of(new com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule("phone", List.of(all),
                        masked)),
                Map.of(), Map.of(), com.ingot.framework.commons.model.iam.FieldMergeMode.GRANTS);
        var local = new AuthorizationDecision(KEY, actor,
                Map.of(READ, new ActionDecision(true, true, List.of(all), read), WRITE,
                        new ActionDecision(true, true, List.of(all), write)),
                "2", Instant.now().plusSeconds(30));
        var json = new ObjectMapper().findAndRegisterModules();
        var wire = json.readValue(json.writeValueAsBytes(local), AuthorizationDecision.class);
        var loaded = RemoteAuthorizationClient.load(remote(request -> {
            return R.ok(wire);
        }), new AuthorizationRequest(KEY, List.of(READ, WRITE)), actor);
        for (var code : List.of(READ, WRITE))
            for (var id : List.of("A", "B")) {
                var target = new com.ingot.framework.commons.model.iam.extension.ScopeTarget(id, id, null, List.of());
                assertEquals(FieldPolicyProcessor.access(FieldPolicyProcessor.forAction(local, code), target),
                        FieldPolicyProcessor.access(FieldPolicyProcessor.forAction(loaded, code), target));
            }
        assertEquals(masked,
                FieldPolicyProcessor
                    .access(FieldPolicyProcessor.forAction(loaded, WRITE),
                            new com.ingot.framework.commons.model.iam.extension.ScopeTarget("A", "A", null, List.of()))
                    .get("phone"));
    }

    @Test
    void cachedReadsAreIdentityBoundButWritesAlwaysLoadFreshAndFailuresDeny() {
        var calls = new AtomicInteger();
        var unavailable = new AtomicReference<>(false);
        var remote = remote(request -> {
            calls.incrementAndGet();
            if (unavailable.get())
                return R.error500();
            return R.ok(decision(RemoteAuthorizationClient.current(), request, Instant.now().plusSeconds(30)));
        });
        var json = new ObjectMapper().findAndRegisterModules();
        var cache = LayeredCacheBuilder.<String, AuthorizationDecision>named("test-only-sdk")
            .settings(LayeredCacheSettings.builder()
                .l1Enabled(true)
                .l2Enabled(false)
                .resilienceEnabled(false)
                .localFloorEnabled(false)
                .build())
            .loader(key -> {
                try {
                    var parsed = json.readValue(key, RemoteAuthorizationClient.CacheKey.class);
                    return RemoteAuthorizationClient.load(remote, parsed.request(), parsed.actor());
                }
                catch (java.io.IOException error) {
                    throw new IllegalStateException(error);
                }
            })
            .build();
        var client = new RemoteAuthorizationClient(remote, registry(), cache, json);
        authenticate("1", "101");
        var read = new AuthorizationRequest(KEY, List.of(READ));
        client.evaluate(read);
        client.evaluate(read);
        assertEquals(1, calls.get());
        authenticate("2", "102");
        client.evaluate(read);
        assertEquals(2, calls.get());
        client.evaluate(new AuthorizationRequest(KEY, List.of(WRITE)));
        client.evaluate(new AuthorizationRequest(KEY, List.of(WRITE)));
        assertEquals(4, calls.get());
        client.evictAll();
        unavailable.set(true);
        assertEquals(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(),
                assertThrows(BizException.class, () -> client.evaluate(read)).getCode());
        assertThrows(BizException.class, () -> client.evaluate(new AuthorizationRequest(KEY, List.of(WRITE))));
    }

    @Test
    void expiredCacheCannotAllowAndValidResponseCannotSubstituteAnotherIdentity() {
        var actor = authenticate("1", "101");
        var request = new AuthorizationRequest(KEY, List.of(READ));
        var evictions = new AtomicInteger();
        var expired = decision(actor, request, Instant.now().minusSeconds(1));
        LayeredCache<String, AuthorizationDecision> cache = new LayeredCache<>() {
            public AuthorizationDecision get(String key) {
                return expired;
            }

            public void evict(String key) {
                evictions.incrementAndGet();
            }

            public void evictAll() {
            }

            public String name() {
                return "expired-only";
            }
        };
        var client = new RemoteAuthorizationClient(remote(q -> R.ok(expired)), registry(), cache,
                new ObjectMapper().findAndRegisterModules());
        assertThrows(BizException.class, () -> client.evaluate(request));
        assertEquals(1, evictions.get());
        var other = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "2", "102");
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(),
                assertThrows(BizException.class,
                        () -> RemoteAuthorizationClient
                            .load(remote(q -> R.ok(decision(other, q, Instant.now().plusSeconds(30)))), request, actor))
                    .getCode());
    }

    @Test
    void signedObjectRequestStillCannotReplaceOriginalIdentity() throws Exception {
        var actor = authenticate("1", "101");
        var secret = "test-only-resource-service-secret-32bytes";
        var mapper = new ObjectMapper().findAndRegisterModules();
        var endpoint = new ResourceObjectEndpoint(registry(), mapper, secret);
        var query = new ResourceObjectQuery(KEY,
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "2", "102"), ObjectQueryPurpose.ASSIGNMENT,
                null, List.of(), null, 1, 20, false, null, null);
        var signed = ResourceRpcSigner.sign(mapper.writeValueAsString(new ResourceObjectInvocation(query, List.of())),
                secret);
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(),
                assertThrows(BizException.class, () -> endpoint.query(signed)).getCode());
        assertEquals(actor, RemoteAuthorizationClient.current());
    }

    @Test
    void sdkHttpMappingPreservesUnavailableAndDeniedReasons() {
        var handler = new SdkAuthorizationErrorHandler();
        assertEquals(503,
                handler.handle(new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE))
                    .getStatusCode()
                    .value());
        assertEquals(403,
                handler.handle(new SdkAuthorizationException(IamReasonCode.ACTION_DENIED)).getStatusCode().value());
        assertEquals(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(),
                handler.handle(new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE))
                    .getBody()
                    .getCode());
    }

    @Test
    void objectRpcSupportsManagementPageSizeAndRejectsInvalidPayload() throws Exception {
        var actor = authenticate("1", "101");
        var secret = "test-only-resource-service-secret-32bytes";
        var mapper = new ObjectMapper().findAndRegisterModules();
        var endpoint = new ResourceObjectEndpoint(registry(), mapper, secret);
        var query = new ResourceObjectQuery(KEY, actor, ObjectQueryPurpose.ASSIGNMENT, null, List.of(), null, 1, 200,
                false, null, null);
        var response = endpoint.query(ResourceRpcSigner
            .sign(mapper.writeValueAsString(new ResourceObjectInvocation(query, List.of())), secret));
        assertTrue(response.isSuccess());
        assertEquals(200, response.getData().candidates().pageSize());
        var oversized = new ResourceObjectQuery(KEY, actor, ObjectQueryPurpose.ASSIGNMENT, null, List.of(), null, 1,
                201, false, null, null);
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(),
                assertThrows(BizException.class,
                        () -> endpoint.query(ResourceRpcSigner.sign(
                                mapper.writeValueAsString(new ResourceObjectInvocation(oversized, List.of())), secret)))
                    .getCode());
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(),
                assertThrows(BizException.class, () -> endpoint.query(ResourceRpcSigner.sign("null", secret)))
                    .getCode());
    }

    private static AuthorizationContext authenticate(String account, String member) {
        var context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, account, member);
        var user = InUser
            .stateless(Long.parseLong(account), null, "web", "standard", UserTypeEnum.ADMIN.getValue(), "fixture",
                    List.of(), List.of(), Map.of())
            .toBuilder()
            .authorizationContext(context)
            .build();
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        return context;
    }

    private static AuthorizationDecision decision(AuthorizationContext actor, AuthorizationRequest request,
            Instant expires) {
        var actions = new java.util.LinkedHashMap<String, ActionDecision>();
        request.actionCodes()
            .forEach(code -> actions.put(code,
                    new ActionDecision(true, true, List.of(new ScopeCondition(true, List.of(), null, List.of())),
                            new FieldPolicyDecision(Map.of(), Map.of(), List.of(), Map.of(),
                                    Map.of(), com.ingot.framework.commons.model.iam.FieldMergeMode.GRANTS))));
        return new AuthorizationDecision(KEY, actor, actions, "1", expires);
    }

    private static RemoteIamAuthorizationService remote(
            java.util.function.Function<AuthorizationRequest, R<AuthorizationDecision>> evaluate) {
        return new RemoteIamAuthorizationService() {
            public R<AuthorizationSnapshotDTO> snapshot(AuthorizationSnapshotRequest request) {
                throw new UnsupportedOperationException();
            }

            public R<AuthorizationDecision> preview(AuthorizationRequest request) { return evaluate.apply(request); }

            public R<AuthorizationDecision> evaluate(AuthorizationRequest request) {
                return evaluate.apply(request);
            }
        };
    }

    private static ResourceRegistry registry() {
        var descriptor = new ResourceDescriptor(KEY,
                List.of(new ActionDescriptor(READ, ExecutionMode.READ_ONLY),
                        new ActionDescriptor(WRITE, ExecutionMode.MUTATING)),
                List.of(ScopeKind.ALL), List.of(), Map.of(), READ, false);
        return new ResourceRegistry(List.of(new ResourceObjectProvider() {
            public ResourceDescriptor descriptor() {
                return descriptor;
            }

            public AuthorizationCandidatePage candidates(ResourceObjectQuery query) {
                return new AuthorizationCandidatePage(List.of(), 0, query.page(), query.pageSize(), true, null);
            }

            public boolean objectsExist(AuthorizationContext actor, List<String> ids) {
                return false;
            }
        }));
    }

}
