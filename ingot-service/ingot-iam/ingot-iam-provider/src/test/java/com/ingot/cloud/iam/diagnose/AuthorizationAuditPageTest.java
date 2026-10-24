package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditCursorPage;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuditReference;
import com.ingot.framework.commons.model.iam.AuditReferenceType;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>用真实 MyBatis 验证审计游标在相同时间戳下不重不漏，并保持域租户边界、筛选和 UTC 半开区间。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthorizationAuditPageTest {
    private static final Instant START = Instant.parse("2026-10-11T01:00:00Z");
    private JdbcTemplate jdbc;
    private AuthorizationAuditService service;
    private AuditReferenceResolver resolver;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource();
        source.setUrl("jdbc:h2:mem:audit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_authorization_audit (id BIGINT PRIMARY KEY, actor_account_id BIGINT, actor_member_id BIGINT, domain VARCHAR(16), tenant_id BIGINT, target_type VARCHAR(64), target_id VARCHAR(128), change_type VARCHAR(64), safe_before VARCHAR(2000), safe_after VARCHAR(2000), revisions VARCHAR(2000), delegation_id BIGINT, assignment_id BIGINT, trace_id VARCHAR(128), occurred_at TIMESTAMP(6))");
        var access = mock(IamAccess.class);
        var evaluator = mock(AuthorizationEvaluator.class);
        when(evaluator.evaluate(any())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(), List.of(), Map.of(), "1", START.plusSeconds(30)));
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_AUDIT_READ))
                .thenReturn(new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", null));
        when(access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_AUDIT_READ))
                .thenReturn(new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "101"), "0", "0", null));
        resolver = mock(AuditReferenceResolver.class);
        when(resolver.resolve(any(), any(), any())).thenReturn(Map.of());
        service = new AuthorizationAuditService(access, evaluator,
                IamMybatisTestAccess.mapper(source, IamAuthorizationAuditMapper.class), resolver);
    }

    @Test
    void cursorWalksTiesWithoutDuplicatesOrGapsAndIgnoresNewerWrites() {
        for (int id = 1; id <= 7; id++) {
            insert(id, "TENANT", 10L, 101, "88", START.plusSeconds(id <= 4 ? 0 : 1));
        }
        List<String> seen = new ArrayList<>();
        AuditCursorPage page = service.list(AuthorizationDomain.TENANT, 3, null, empty());
        page.items().forEach(item -> seen.add(item.record().id()));
        assertTrue(page.hasMore());
        insert(99, "TENANT", 10L, 101, "88", START.plusSeconds(60));
        while (page.hasMore()) {
            page = service.list(AuthorizationDomain.TENANT, 3, page.nextCursor(), empty());
            page.items().forEach(item -> seen.add(item.record().id()));
        }
        assertEquals(List.of("7", "6", "5", "4", "3", "2", "1"), seen);
        assertNull(page.nextCursor());
    }

    @Test
    void filtersAndTenantBoundaryApplyTogetherWithCursor() {
        for (int id = 1; id <= 7; id++) {
            String domain = id == 6 ? "PLATFORM" : "TENANT";
            Long tenant = id == 6 ? null : id == 5 ? 20L : 10L;
            String target = id == 4 ? "other" : "88";
            Instant time = id == 3 ? START.plusSeconds(2) : START.plusSeconds(id == 2 ? 1 : 0);
            insert(id, domain, tenant, id == 7 ? 102 : 101, target, time);
        }
        var query = AuthorizationAuditQuery.of("101", "resource", "88", "UPDATE",
                Instant.parse("2026-10-11T09:00:00+08:00"), START.plusSeconds(2));
        AuditCursorPage first = service.list(AuthorizationDomain.TENANT, 1, null, query);
        assertEquals("2", first.items().getFirst().record().id());
        assertEquals("[]", first.items().getFirst().record().after().get(AuditField.FIELD_CAPABILITIES));
        assertTrue(first.hasMore());
        AuditCursorPage second = service.list(AuthorizationDomain.TENANT, 1, first.nextCursor(), query);
        assertEquals("1", second.items().getFirst().record().id());
        assertFalse(second.hasMore());
        AuditCursorPage reused = service.list(AuthorizationDomain.PLATFORM, 20, first.nextCursor(), query);
        assertEquals(List.of("6"), reused.items().stream().map(item -> item.record().id()).toList());
        AuditCursorPage platformAll = service.list(AuthorizationDomain.PLATFORM, 20, null, query);
        assertEquals("6", platformAll.items().getFirst().record().id());
        assertNull(platformAll.items().getFirst().record().context().tenantId());
    }

    @Test
    void visibleActorNameIsCopiedFromReferences() {
        insert(1, "TENANT", 10L, 101, "88", START);
        when(resolver.resolve(any(), any(), any())).thenReturn(Map.of(AuditReferenceType.MEMBER.key("101"),
                AuditReference.visible(AuditReferenceType.MEMBER, "101", "管理者")));
        AuditCursorPage page = service.list(AuthorizationDomain.TENANT, 20, null, empty());
        assertEquals("管理者", page.items().getFirst().record().actor().displayName());
        assertEquals(1, page.references().size());
    }

    @Test
    void rejectsInvalidLimitAndCursor() {
        assertInvalid(() -> service.list(AuthorizationDomain.TENANT, 0, null, empty()));
        assertInvalid(() -> service.list(AuthorizationDomain.TENANT, AuthorizationAuditService.MAX_LIMIT + 1, null, empty()));
        assertInvalid(() -> service.list(AuthorizationDomain.TENANT, 20, "%%%", empty()));
        assertInvalid(() -> service.list(AuthorizationDomain.TENANT, 20, "x".repeat(AuditCursor.MAX_LENGTH + 1), empty()));
    }

    @Test
    void cursorRoundTripsMicrosecondsAndRejectsMalformedPayloads() {
        var cursor = new AuditCursor(LocalDateTime.of(2026, 10, 11, 1, 0, 0, 123_456_000), BigInteger.valueOf(42));
        assertEquals(cursor, AuditCursor.decode(cursor.encode()));
        assertNull(AuditCursor.decode(" "));
        java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        for (String plain : List.of("abc", "1.0", "1.-2", "1", ".1", "1.01", "99999999999999999999.1")) {
            assertInvalid(() -> AuditCursor.decode(encoder.encodeToString(plain.getBytes())));
        }
    }

    @Test
    void rejectsUnknownTypesInvalidIdentifiersOverlongConditionsAndReversedTime() {
        for (String id : List.of("0", "-1", "+1", "01", "abc", "9223372036854775808")) {
            assertInvalid(() -> AuthorizationAuditQuery.of(id, null, null, null, null, null));
        }
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, null, "UNKNOWN", null, null));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, "x".repeat(65), null, null, null, null));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, "x".repeat(129), null, null, null));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, null, null, START, START));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, null, null, START, START.minusSeconds(1)));
        assertNull(AuthorizationAuditQuery.of(" ", " ", " ", " ", null, null).actorMemberId());
    }

    private void insert(int id, String domain, Long tenant, int actor, String target, Instant time) {
        jdbc.update("INSERT INTO iam_authorization_audit VALUES (?, 1, ?, ?, ?, 'resource', ?, 'UPDATE', '{}', ?, '{}', NULL, NULL, NULL, ?)",
                id, actor, domain, tenant, target, "{\"FIELD_CAPABILITIES\":[]}", LocalDateTime.ofInstant(time, ZoneOffset.UTC));
    }

    private static AuthorizationAuditQuery empty() {
        return new AuthorizationAuditQuery(null, null, null, null, null, null);
    }

    private static void assertInvalid(Runnable operation) {
        BizException exception = assertThrows(BizException.class, operation::run);
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), exception.getCode());
    }
}
