package com.ingot.framework.authorization.field;

import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证来源期限不被派生授权延长，以及服务清单签名的身份和用途边界。</p>
 * @author jy
 * @since 1.0.0
 */
class FieldPolicyBoundaryTest {
    private static final ResourceKey RESOURCE = MemberResources.PLATFORM_MEMBER;
    private static final String READ = IamAction.VALUE_PLATFORM_MEMBER_READ;
    private static final String SECRET = "manifest-test-secret-at-least-32-bytes";

    @Test
    void derivedDecisionInheritsEarlierSourceExpiryAndExpiredPolicyIsRejected() throws Exception {
        Instant deadline = Instant.now().minusSeconds(1);
        var fields = new FieldPolicyDecision(Map.of(), Map.of(), List.of(), Map.of(), Map.of(), FieldMergeMode.GRANTS, deadline);
        var decision = new AuthorizationDecision(RESOURCE, null, Map.of(READ, new ActionDecision(true, true, List.of(), fields)),
                "version", Instant.now().plusSeconds(30));
        assertEquals(deadline, decision.expiresAt());
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.forAction(decision, READ));
        var mapper = new ObjectMapper().findAndRegisterModules();
        assertEquals(deadline, mapper.readValue(mapper.writeValueAsBytes(fields), FieldPolicyDecision.class).expiresAt());
        assertThrows(Exception.class, () -> mapper.readValue("{\"defaults\":{},\"ceilings\":{},\"rules\":[],\"operations\":{},\"masks\":{}}", FieldPolicyDecision.class));
    }

    @Test
    void manifestRequiresDedicatedSignatureAllowedServiceAndFixedPurpose() throws Exception {
        var mapper = new ObjectMapper();
        var bindings = new FieldBindingRegistry(mapper);
        bindings.register(Row.class, RESOURCE, READ, FieldUse.READ);
        var provider = new ResourceObjectProvider() {
            public ResourceDescriptor descriptor() { return new ResourceDescriptor(RESOURCE,
                    List.of(new ActionDescriptor(READ, ExecutionMode.READ_ONLY)), List.of(), List.of(), Map.of(), READ, false); }
            public AuthorizationCandidatePage candidates(ResourceObjectQuery query) { throw new AssertionError("清单不得查询业务原值"); }
            public boolean objectsExist(AuthorizationContext context, List<String> ids) { throw new AssertionError("清单不得查询对象事实"); }
        };
        var properties = new FieldManifestProperties(); properties.setSecret(SECRET);
        var endpoint = new FieldManifestEndpoint(bindings, new ResourceRegistry(List.of(provider)), mapper, properties);
        var request = new FieldManifestInvocation(FieldManifestInvocation.PURPOSE, FieldManifestInvocation.IAM_CALLER, RESOURCE);
        var signed = ResourceRpcSigner.sign(mapper.writeValueAsString(request), SECRET);
        var manifest = endpoint.manifest(signed).getData();
        assertEquals(RESOURCE, manifest.resource());
        assertEquals("phone", manifest.bindings().getFirst().fieldKey());
        assertFalse(mapper.writeValueAsString(manifest).contains("business-secret"));
        assertThrows(SdkAuthorizationException.class, () -> endpoint.manifest(ResourceRpcSigner.sign(signed.payload(), SECRET + "wrong")));
        for (var invalid : List.of(new FieldManifestInvocation("wrong", FieldManifestInvocation.IAM_CALLER, RESOURCE),
                new FieldManifestInvocation(FieldManifestInvocation.PURPOSE, "untrusted", RESOURCE))) {
            var envelope = ResourceRpcSigner.sign(mapper.writeValueAsString(invalid), SECRET);
            assertThrows(SdkAuthorizationException.class, () -> endpoint.manifest(envelope));
        }
    }

    /** 纯字段元数据，不参与业务原值读取。 */
    private record Row(@FieldBinding(key = "phone") String aphone) { }
}
