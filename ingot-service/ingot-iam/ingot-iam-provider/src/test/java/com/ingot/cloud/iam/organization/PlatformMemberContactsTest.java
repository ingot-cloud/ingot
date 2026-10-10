package com.ingot.cloud.iam.organization;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.RoleFieldPermissionService;
import com.ingot.cloud.iam.group.GroupService;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.GroupRepository;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.jackson.InApiTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>使用真实成员持久化验证平台资料与详情时间来源、账号隔离、字段写入门禁及事务回滚。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformMemberContactsTest {
    private JdbcTemplate jdbc;
    private MemberQueryService service;
    private RoleFieldPermissionService policies;
    private IamAuditWriter audits;
    private AssignmentService assignments;
    private MemberQueryRepository members;
    private ResourceAccess scopes;

    @BeforeEach
    void database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY,username VARCHAR(64),phone VARCHAR(32),email VARCHAR(128),version BIGINT,deleted_at TIMESTAMP,last_login_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,account_id BIGINT,display_name VARCHAR(128),avatar VARCHAR(512),phone VARCHAR(32),email VARCHAR(128),status VARCHAR(16),version BIGINT,created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY,tenant_id BIGINT,account_id BIGINT,phone VARCHAR(32),email VARCHAR(128))");
        jdbc.update("INSERT INTO iam_account VALUES(1,'alice','13800000001','login@example.com',7,NULL,NULL)");
        jdbc.update("INSERT INTO iam_platform_member VALUES(1001,1,'平台成员',NULL,'13900000001','contact@example.com','ACTIVE',0,NULL,NULL)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES(101,10,1,'13700000001','tenant@example.com')");
        var access = mock(IamAccess.class);
        var actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"), "0", "0", null);
        when(access.require(eq(AuthorizationDomain.PLATFORM), any())).thenReturn(actor);
        when(access.nextId()).thenReturn(5001L);
        var capabilities = mock(ObjectCapabilities.class);
        when(capabilities.platformMember(any(), anyString())).thenReturn(Map.of());
        var fields = mock(FieldAccessEvaluator.class);
        // 字段投影由专用回归覆盖；此处核对进入投影前的成员资料来源。
        when(fields.project(any(MemberRecord.class), anyMap(), any(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));
        policies = mock(RoleFieldPermissionService.class);
        policy(new FieldAccess(FieldVisibility.FULL, true));
        audits = mock(IamAuditWriter.class);
        assignments = mock(AssignmentService.class);
        scopes = mock(ResourceAccess.class);
        members = spy(IamMybatisTestAccess.memberQueries(source));
        service = new MemberQueryService(access, scopes, capabilities, fields, audits,
                members, mock(GroupRepository.class), assignments,
                mock(GroupService.class), new DataSourceTransactionManager(source), policies, com.ingot.cloud.iam.persistence.FieldTestSupport.projection(), new com.ingot.framework.authorization.field.FieldWriteExecutor(new com.ingot.framework.authorization.field.FieldPolicyProvider() {
                    public com.ingot.framework.commons.model.iam.extension.AuthorizationDecision read(
                            com.ingot.framework.commons.model.iam.extension.ResourceKey resource, String action) { return write(resource, action); }
                    public com.ingot.framework.commons.model.iam.extension.AuthorizationDecision write(
                            com.ingot.framework.commons.model.iam.extension.ResourceKey resource, String action) {
                        return new com.ingot.framework.commons.model.iam.extension.AuthorizationDecision(resource, actor.context(),
                                Map.of(action, new com.ingot.framework.commons.model.iam.extension.ActionDecision(true, true,
                                        List.of(new com.ingot.framework.commons.model.iam.extension.ScopeCondition(true, List.of(), null, List.of())),
                                        policies.evaluate(resource, actor.context(), IamAction.PLATFORM_MEMBER_UPDATE))),
                                "current", java.time.Instant.now().plusSeconds(30));
                    }
                }));
    }

    @Test
    void detailUsesMemberTimesAndGlobalAccountLoginAsUtcInstants() throws Exception {
        jdbc.update("UPDATE iam_platform_member SET created_at=?,updated_at=? WHERE id=1001",
                LocalDateTime.parse("2026-10-01T01:02:03"), LocalDateTime.parse("2026-10-08T04:05:06"));
        jdbc.update("UPDATE iam_account SET last_login_at=? WHERE id=1", LocalDateTime.parse("2026-09-30T07:08:09"));
        var member = service.get(AuthorizationDomain.PLATFORM, "1001").record();
        assertEquals(Instant.parse("2026-10-01T01:02:03Z"), member.joinedAt());
        assertEquals(Instant.parse("2026-10-08T04:05:06Z"), member.updatedAt());
        assertEquals(Instant.parse("2026-09-30T07:08:09Z"), member.lastLoginAt());
        var json = new ObjectMapper().registerModule(new InApiTimeModule()).valueToTree(member);
        assertEquals("2026-10-01T01:02:03Z", json.path("joinedAt").asText());
        assertEquals("2026-09-30T07:08:09Z", json.path("lastLoginAt").asText());
        assertEquals("2026-10-08T04:05:06Z", json.path("updatedAt").asText());
        var changed = service.patch(AuthorizationDomain.PLATFORM, "1001",
                new MemberProfileInput("0", null, null, "13900000002", null)).record();
        assertEquals(member.joinedAt(), changed.joinedAt());
        assertEquals(member.lastLoginAt(), changed.lastLoginAt());
        assertTrue(changed.updatedAt().isAfter(member.updatedAt()));
    }

    @Test
    void missingLoginIsOmittedAndDeletedAccountDoesNotExposeLoginInformation() {
        var mapper = new ObjectMapper().registerModule(new InApiTimeModule());
        var member = service.get(AuthorizationDomain.PLATFORM, "1001").record();
        assertNull(member.lastLoginAt());
        assertFalse(mapper.valueToTree(member).has("lastLoginAt"));
        jdbc.update("UPDATE iam_account SET last_login_at=?,deleted_at=? WHERE id=1",
                LocalDateTime.parse("2026-10-08T01:00:00"), LocalDateTime.parse("2026-10-08T02:00:00"));
        var removed = service.get(AuthorizationDomain.PLATFORM, "1001").record();
        assertNull(removed.username());
        assertNull(removed.lastLoginAt());
    }

    @Test
    void invisibleMemberCannotReadMemberOrAccountTimes() {
        doThrow(new BizException(IamReasonCode.OBJECT_NOT_FOUND)).when(scopes)
                .requireVisibleMember(any(), eq(IamAction.PLATFORM_MEMBER_READ), eq(1001L));
        assertThrows(BizException.class, () -> service.get(AuthorizationDomain.PLATFORM, "1001"));
        verifyNoInteractions(members);
    }

    @Test
    void timeFieldsAreRejectedEvenWhenExplicitlyNull() throws Exception {
        var mapper = new ObjectMapper();
        for (var field : List.of("joinedAt", "lastLoginAt", "updatedAt")) {
            var input = mapper.readValue("{\"expectedVersion\":\"0\",\"" + field + "\":null}", MemberProfileInput.class);
            var rejected = assertThrows(BizException.class,
                    () -> service.patch(AuthorizationDomain.PLATFORM, "1001", input));
            assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), rejected.getCode());
        }
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_platform_member WHERE id=1001", Long.class));
        verifyNoInteractions(audits);
    }

    @Test
    void readsMemberContactsIndependentlyOfAccountLoginInformation() {
        var member = service.get(AuthorizationDomain.PLATFORM, "1001").record();
        assertEquals("13900000001", member.phone());
        assertEquals("contact@example.com", member.email());
        assertEquals("alice", member.username());
        jdbc.update("UPDATE iam_account SET phone='13800000002',email='changed@example.com' WHERE id=1");
        assertEquals("13900000001", service.get(AuthorizationDomain.PLATFORM, "1001").record().phone());
    }

    @Test
    void editsAndClearsOnlyMemberContactsWithoutAccountFallback() {
        var changed = service.patch(AuthorizationDomain.PLATFORM, "1001",
                new MemberProfileInput("0", null, null, "13900000002", "new-contact@example.com"));
        assertEquals("1", changed.version());
        assertEquals("13900000002", changed.record().phone());
        var cleared = service.patch(AuthorizationDomain.PLATFORM, "1001",
                new MemberProfileInput("1", null, null, "", ""));
        assertNull(cleared.record().phone());
        assertNull(cleared.record().email());
        assertEquals("13800000001", jdbc.queryForObject("SELECT phone FROM iam_account WHERE id=1", String.class));
        assertEquals("login@example.com", jdbc.queryForObject("SELECT email FROM iam_account WHERE id=1", String.class));
        assertEquals(7L, jdbc.queryForObject("SELECT version FROM iam_account WHERE id=1", Long.class));
        assertEquals("13700000001", jdbc.queryForObject("SELECT phone FROM iam_tenant_member WHERE id=101", String.class));
        assertEquals("tenant@example.com", jdbc.queryForObject("SELECT email FROM iam_tenant_member WHERE id=101", String.class));
    }

    @Test
    void explicitNullFromJsonClearsOnlySubmittedContact() throws Exception {
        var input = new ObjectMapper().readValue("{\"expectedVersion\":\"0\",\"phone\":null}", MemberProfileInput.class);
        var changed = service.patch(AuthorizationDomain.PLATFORM, "1001", input);
        assertNull(changed.record().phone());
        assertEquals("contact@example.com", changed.record().email());
        assertNull(jdbc.queryForObject("SELECT phone FROM iam_platform_member WHERE id=1001", String.class));
        assertEquals("contact@example.com", jdbc.queryForObject("SELECT email FROM iam_platform_member WHERE id=1001", String.class));
        assertEquals("13800000001", jdbc.queryForObject("SELECT phone FROM iam_account WHERE id=1", String.class));
    }

    @Test
    void maskedAndReadonlyFieldsStillRejectContactChanges() {
        for (var field : List.of(new FieldAccess(FieldVisibility.MASKED, false), new FieldAccess(FieldVisibility.FULL, false))) {
            policy(field);
            assertThrows(SdkAuthorizationException.class, () -> service.patch(AuthorizationDomain.PLATFORM, "1001",
                    new MemberProfileInput("0", null, null, "13900000002", null)));
        }
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_platform_member WHERE id=1001", Long.class));
        assertEquals("13900000001", jdbc.queryForObject("SELECT phone FROM iam_platform_member WHERE id=1001", String.class));
        verifyNoInteractions(audits);
    }

    @Test
    void staleMemberVersionRejectsAndAuditFailureRollsBack() {
        assertThrows(BizException.class, () -> service.patch(AuthorizationDomain.PLATFORM, "1001",
                new MemberProfileInput("9", null, null, "13900000002", null)));
        doThrow(new IllegalStateException("审计失败")).when(audits)
            .write(any(), anyLong(), anyString(), anyString(), any(), anyMap(), anyMap(), anyMap());
        assertThrows(IllegalStateException.class, () -> service.patch(AuthorizationDomain.PLATFORM, "1001",
                new MemberProfileInput("0", null, null, "13900000002", null)));
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_platform_member WHERE id=1001", Long.class));
        assertEquals("13900000001", jdbc.queryForObject("SELECT phone FROM iam_platform_member WHERE id=1001", String.class));
    }

    @Test
    void memberAndRoleDeltasCommitTogetherAndFailureRollsBackBoth() {
        jdbc.execute("CREATE TABLE role_delta(id BIGINT PRIMARY KEY)");
        var delta = new MemberRoleChanges(List.of(new MemberRoleAssignmentDraft("7",
                new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "8"), Map.of(), null, null)), List.of(), List.of());
        var input = new PlatformMemberEditInput("0", null, null, "13900000002", null, delta, java.util.Set.of("phone"));
        doAnswer(call -> { jdbc.update("INSERT INTO role_delta VALUES(1)");
            throw new BizException(IamReasonCode.REVISION_CONFLICT); }).when(assignments).applyMemberRoleChanges("1001", delta);
        assertThrows(BizException.class, () -> service.patchPlatform("1001", input));
        assertEquals("13900000001", jdbc.queryForObject("SELECT phone FROM iam_platform_member WHERE id=1001", String.class));
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_platform_member WHERE id=1001", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM role_delta", Integer.class));
        doAnswer(call -> { jdbc.update("INSERT INTO role_delta VALUES(1)"); return null; })
                .when(assignments).applyMemberRoleChanges("1001", delta);
        assertEquals("1", service.patchPlatform("1001", input).version());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM role_delta", Integer.class));
    }

    @Test
    void roleOnlyEditDoesNotRequireAnyWritableProfileFieldAndPreviewDoesNotWrite() {
        policy(new FieldAccess(FieldVisibility.HIDDEN, false));
        var delta = new MemberRoleChanges(List.of(), List.of(), List.of(new MemberRoleRemoval("81", "2")));
        when(assignments.previewMemberRoleChanges("1001", delta)).thenReturn(List.of());
        var input = new PlatformMemberEditInput("0", null, null, null, null, delta, java.util.Set.of("expectedVersion"));
        var preview = service.previewPlatformEdit("1001", input);
        assertTrue(preview.valid());
        assertEquals(1, preview.effectiveResult().removals());
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_platform_member WHERE id=1001", Long.class));
        assertEquals("1", service.patchPlatform("1001", input).version());
        verify(assignments).applyMemberRoleChanges("1001", delta);
        assertEquals("13900000001", jdbc.queryForObject("SELECT phone FROM iam_platform_member WHERE id=1001", String.class));
    }

    private void policy(FieldAccess field) {
        var values = Map.of(MemberFieldKey.VALUE_PHONE, field, MemberFieldKey.VALUE_EMAIL, field,
                MemberTimeFields.JOINED_AT, new FieldAccess(FieldVisibility.FULL, false),
                MemberTimeFields.LAST_LOGIN_AT, new FieldAccess(FieldVisibility.FULL, false),
                MemberTimeFields.UPDATED_AT, new FieldAccess(FieldVisibility.FULL, false));
        var rules = values.entrySet().stream().map(entry -> new com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule(
                entry.getKey(), List.of(new com.ingot.framework.commons.model.iam.extension.ScopeCondition(true, List.of(), null, List.of())), entry.getValue())).toList();
        var decision = new FieldPolicyDecision(values, values, rules, Map.of(), Map.of(), FieldMergeMode.GRANTS);
        when(policies.evaluate(any(), any(), any())).thenReturn(decision);
        when(policies.previewAll(any(), any(), anyList()))
            .thenReturn(Map.of(IamAction.PLATFORM_MEMBER_READ, decision, IamAction.PLATFORM_MEMBER_UPDATE, decision));
    }
}
