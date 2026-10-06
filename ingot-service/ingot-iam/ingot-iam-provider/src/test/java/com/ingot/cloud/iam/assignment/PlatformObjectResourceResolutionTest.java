package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRepository;
import com.ingot.cloud.iam.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamDelegationActionCeilingEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleParameterEntity;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamCapabilities;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>对象类型由真实应用和资源关联确定，全部关联操作一致且委派上限共同生效。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformObjectResourceResolutionTest {
    private static final String PARAMETER = "objects";
    private final ActiveIdentity actor = new ActiveIdentity(
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "99"), "0", "0", "0");
    private final IamAccess access = mock(IamAccess.class);
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final DelegationRepository delegations = mock(DelegationRepository.class);
    private final RoleRepository roleStore = mock(RoleRepository.class);
    private final RoleService roles = mock(RoleService.class);
    private final AuthorizationCandidateMapper candidates = mock(AuthorizationCandidateMapper.class);
    private final PlatformAuthorizationEditor editor = new PlatformAuthorizationEditor(access, evaluator,
            assignments, delegations, roleStore, roles, candidates, new com.ingot.cloud.iam.extension.BuiltinResourceProviders(candidates).registry(List.of()));

    @org.junit.jupiter.api.BeforeEach
    void currentIdentity() { when(access.requireCurrent()).thenReturn(actor); }

    @ParameterizedTest
    @ValueSource(strings = {"aaaa", "iam-platform:application:create", "iam-platform:role:read"})
    void replayDiagnosisAndWritesResolveRealResourceInsteadOfActionCode(String code) {
        when(access.requireCurrent()).thenReturn(actor);
        var action = row("331", code, "iam-platform", "application");
        prepareVersion(List.of(action));
        assertTrue(replay(null).supported());
        var parameter = new IamRoleParameterEntity();
        parameter.setParameterKey(PARAMETER);
        parameter.setBindingKind(ScopeBindingKind.OBJECTS);
        when(roleStore.listParameters(31)).thenReturn(List.of(parameter));
        when(candidates.count(any(AuthorizationCandidateSql.Query.class))).thenReturn(1L);
        assertTrue(editor.validateBindings(input(null)).isEmpty());
        assertTrue(editor.validCeilingObjects(ceiling("331")));
        prepareDiagnosis();
        assertTrue(editor.diagnose(AuthorizationCandidateKind.OBJECT, "331", "10", "", List.of(), 1, 20).supported());
        var query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates, times(2)).page(query.capture());
        query.getAllValues().forEach(value -> assertEquals("application", value.objectResource()));
        verify(candidates, times(4)).count(query.capture());
        query.getAllValues().forEach(value -> assertEquals("application", value.objectResource()));
    }

    @Test
    void sharedParameterDoesNotDependOnOperationOrder() {
        var custom = row("331", "aaaa", "iam-platform", "application");
        var builtin = row("332", IamAction.VALUE_PLATFORM_APPLICATION_CREATE, "iam-platform", "application");
        prepareVersion(List.of(custom, builtin));
        assertTrue(replay(null).supported());
        when(candidates.actions(anyList())).thenReturn(List.of(builtin, custom));
        assertTrue(replay(null).supported());
        var query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates, times(2)).page(query.capture());
        query.getAllValues().forEach(value -> assertEquals("application", value.objectResource()));
    }

    @ParameterizedTest
    @CsvSource({"other-platform,application", "iam-platform,unknown-resource"})
    void coreActionCodeCannotSpoofUnregisteredApplicationOrResource(String appCode, String resourceCode) {
        prepareVersion(List.of(row("331", IamAction.VALUE_PLATFORM_APPLICATION_CREATE, appCode, resourceCode)));
        assertFalse(replay(null).supported());
        assertFalse(editor.validCeilingObjects(ceiling("331")));
        prepareDiagnosis();
        assertFalse(editor.diagnose(AuthorizationCandidateKind.OBJECT, "331", "10", "", List.of(), 1, 20).supported());
        verify(candidates, never()).page(any());
        verify(candidates, never()).count(any());
    }

    @Test
    void everyAssociatedOperationMustResolveTheSameRegisteredResource() {
        var registered = row("331", IamAction.VALUE_PLATFORM_APPLICATION_CREATE, "iam-platform", "application");
        var unavailable = row("332", IamAction.VALUE_PLATFORM_APPLICATION_READ, "iam-platform", "unknown-resource");
        prepareVersion(List.of(registered, unavailable));
        assertFalse(replay(null).supported());
        when(candidates.actions(anyList())).thenReturn(List.of(unavailable, registered));
        assertFalse(replay(null).supported());
        verify(candidates, never()).page(any());
    }

    @Test
    void crossResourceParameterAndMissingOperationsFailClosed() {
        var application = row("331", "aaaa", "iam-platform", "application");
        var member = new AuthorizationCandidateMapper.ActionRow(BigInteger.valueOf(332), "查看成员",
                "iam-platform:member:read", BigInteger.TEN, "平台", "iam-platform",
                BigInteger.valueOf(21), "成员", "member", "[]");
        prepareVersion(List.of(application, member));
        assertFalse(replay(null).supported());
        when(candidates.actions(anyList())).thenReturn(List.of(application));
        assertFalse(replay(null).supported());
        verify(candidates, never()).page(any());
    }

    @Test
    void crossApplicationMetadataCannotBorrowCoreResourceIdentity() {
        var first = row("331", "aaaa", "iam-platform", "application");
        var other = new AuthorizationCandidateMapper.ActionRow(BigInteger.valueOf(332), "创建",
                IamAction.VALUE_PLATFORM_APPLICATION_CREATE, BigInteger.valueOf(11), "其他应用", "iam-platform",
                BigInteger.valueOf(20), "应用", "application", "[]");
        prepareVersion(List.of(first, other));
        assertFalse(replay(null).supported());
        verify(candidates, never()).page(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[\"101\",\"102\"]", "[\"103\"]"})
    void allDelegationCeilingsStillIntersectAfterResourceResolution(String secondObjects) {
        prepareVersion(List.of(row("331", "aaaa", "iam-platform", "application"),
                row("332", IamAction.VALUE_PLATFORM_APPLICATION_CREATE, "iam-platform", "application")));
        var source = new IamDelegationGrantEntity();
        source.setId(BigInteger.valueOf(60));
        source.setPlatformAdministratorId(BigInteger.valueOf(99));
        source.setStatus(GrantStatus.ACTIVE);
        source.setMaxAssignmentDurationSeconds(3600L);
        source.setMaxAssignmentDurationNanos(0);
        var revision = new IamDelegationRoleRevisionEntity();
        revision.setRevisionId(BigInteger.valueOf(31));
        revision.setRevisionKind(RoleKind.PLATFORM_CUSTOM);
        when(delegations.find(AuthorizationDomain.PLATFORM, null, 60)).thenReturn(source);
        when(delegations.children(AuthorizationDomain.PLATFORM, List.of(source.getId()))).thenReturn(Map.of(
                source.getId(), new DelegationRepository.Children(List.of(revision), List.of(), List.of(),
                        List.of(storedCeiling("331", "[\"100\",\"101\"]"), storedCeiling("332", secondObjects)))));
        assertTrue(replay("60").supported());
        var query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals(secondObjects.contains("101") ? List.of(BigInteger.valueOf(101)) : List.of(),
                query.getValue().allowedIds());
        assertEquals("application", query.getValue().objectResource());
    }

    @Test
    void delegationCandidatesResolveCustomOperationByItsResourceAssociation() {
        when(access.requireCurrent()).thenReturn(actor);
        when(access.capabilities(eq(actor), anyCollection())).thenReturn(new IamCapabilities(Map.of(
                IamAction.PLATFORM_DELEGATION_CREATE, new IamActionAuthorizer.Admission(true))));
        when(candidates.actions(anyList())).thenReturn(List.of(row("331", "aaaa", "iam-platform", "application")));
        assertTrue(editor.delegations(AuthorizationCandidateKind.OBJECT, "331", "", List.of(), 1, 20).supported());
        var query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals("application", query.getValue().objectResource());
    }

    private void prepareVersion(List<AuthorizationCandidateMapper.ActionRow> actions) {
        var revision = new IamRoleRevisionJoin();
        revision.setDomain(AuthorizationDomain.PLATFORM);
        revision.setEnabled(true);
        when(assignments.findRevision(31)).thenReturn(revision);
        when(roles.synthesizedGrants(31)).thenReturn(actions.stream().map(action -> new ActionGrant(
                action.id().toString(), List.of(new ScopeExpression(ScopeKind.OBJECT_SET, PARAMETER, false)))).toList());
        when(candidates.actions(anyList())).thenReturn(actions);
    }

    private AuthorizationCandidatePage replay(String source) {
        return editor.selectedAssignmentCandidates(actor, "90", input(source), AuthorizationCandidateKind.OBJECT,
                PARAMETER, 1, 20);
    }

    private void prepareDiagnosis() {
        when(access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE)).thenReturn(actor);
        when(evaluator.evaluate(actor.context())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_PLATFORM_APPLICATION_READ), List.of(), Map.of(), "0", Instant.now().plusSeconds(30)));
    }

    private static AssignmentInput input(String source) {
        return new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "1"),
                new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "31"),
                Map.of(PARAMETER, new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("100"))), null, null, source);
    }

    private static ActionScopeCeiling ceiling(String actionId) {
        return new ActionScopeCeiling(actionId, List.of(new ScopeExpression(ScopeKind.OBJECT_SET, PARAMETER, false)),
                Map.of(PARAMETER, new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("100"))));
    }

    private static IamDelegationActionCeilingEntity storedCeiling(String actionId, String objects) {
        var ceiling = new IamDelegationActionCeilingEntity();
        ceiling.setActionId(new BigInteger(actionId));
        ceiling.setScopes("[{\"kind\":\"OBJECT_SET\",\"parameterKey\":\"objects\"}]");
        ceiling.setScopeBindings("{\"objects\":{\"kind\":\"OBJECTS\",\"ids\":" + objects + "}}");
        return ceiling;
    }

    private static AuthorizationCandidateMapper.ActionRow row(String id, String code, String appCode, String resourceCode) {
        return new AuthorizationCandidateMapper.ActionRow(new BigInteger(id), "创建", code, BigInteger.TEN, "平台",
                appCode, BigInteger.valueOf(20), "应用", resourceCode, "[]");
    }
}
