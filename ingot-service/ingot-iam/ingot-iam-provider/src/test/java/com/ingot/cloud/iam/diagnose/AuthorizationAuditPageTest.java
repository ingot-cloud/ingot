package com.ingot.cloud.iam.diagnose;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>用真实 MyBatis 分页验证审计的域租户边界、筛选计数及 UTC 时间半开区间。</p>
 * @author jy
 * @since 1.0.0
 */
class AuthorizationAuditPageTest {
    @Test
    void filtersBeforePaginationAndCountWithinTenantAndPlatform() {
        var source = new DriverManagerDataSource();
        source.setUrl("jdbc:h2:mem:audit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_authorization_audit (id BIGINT PRIMARY KEY, actor_account_id BIGINT, actor_member_id BIGINT, domain VARCHAR(16), tenant_id BIGINT, target_type VARCHAR(64), target_id VARCHAR(128), change_type VARCHAR(64), safe_before VARCHAR(2000), safe_after VARCHAR(2000), revisions VARCHAR(2000), delegation_id BIGINT, assignment_id BIGINT, trace_id VARCHAR(128), occurred_at TIMESTAMP(6))");
        var start = Instant.parse("2026-10-11T01:00:00Z");
        for (int id = 1; id <= 7; id++) {
            String domain = id == 6 ? "PLATFORM" : "TENANT";
            Long tenant = id == 6 ? null : id == 5 ? 20L : 10L;
            String target = id == 4 ? "other" : "88";
            Instant time = id == 3 ? start.plusSeconds(2) : start.plusSeconds(id == 2 ? 1 : 0);
            jdbc.update("INSERT INTO iam_authorization_audit VALUES (?, 1, ?, ?, ?, 'resource', ?, 'UPDATE', '{}', ?, '{}', NULL, NULL, NULL, ?)",
                    id, id == 7 ? 102 : 101, domain, tenant, target,
                    "{\"FIELD_CAPABILITIES\":[]}", LocalDateTime.ofInstant(time, ZoneOffset.UTC));
        }
        var access = mock(IamAccess.class);
        var evaluator = mock(AuthorizationEvaluator.class);
        when(evaluator.evaluate(any())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(), List.of(), Map.of(), "1", start.plusSeconds(30)));
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_AUDIT_READ))
                .thenReturn(new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", null));
        when(access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_AUDIT_READ))
                .thenReturn(new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "101"), "0", "0", null));
        var service = new DiagnoseAuditService(access, evaluator, mock(AuthorizationEvaluationRepository.class),
                mock(ResourceAccess.class), mock(FieldAccessEvaluator.class),
                IamMybatisTestAccess.mapper(source, IamAuthorizationAuditMapper.class),
                mock(IamPlatformMemberMapper.class), mock(IamTenantMemberMapper.class), mock(IamApplicationMapper.class));
        var query = AuthorizationAuditQuery.of("101", "resource", "88", "UPDATE",
                Instant.parse("2026-10-11T09:00:00+08:00"), start.plusSeconds(2));
        var first = service.listAudits(AuthorizationDomain.TENANT, 1, 1, query);
        assertEquals(2, first.total());
        assertEquals("2", first.items().getFirst().record().id());
        assertEquals("[]", first.items().getFirst().record().after().get(AuditField.FIELD_CAPABILITIES));
        var second = service.listAudits(AuthorizationDomain.TENANT, 2, 1, query);
        assertEquals(2, second.total());
        assertEquals("1", second.items().getFirst().record().id());
        var platform = service.listAudits(AuthorizationDomain.PLATFORM, 1, 20, query);
        assertEquals(1, platform.total());
        assertEquals("6", platform.items().getFirst().record().id());
        assertNull(platform.items().getFirst().record().context().tenantId());
    }

    @Test
    void rejectsUnknownTypesInvalidIdentifiersOverlongConditionsAndReversedTime() {
        for (String id : List.of("0", "-1", "+1", "01", "abc", "9223372036854775808")) {
            assertInvalid(() -> AuthorizationAuditQuery.of(id, null, null, null, null, null));
        }
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, null, "UNKNOWN", null, null));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, "x".repeat(65), null, null, null, null));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, "x".repeat(129), null, null, null));
        var time = Instant.parse("2026-10-11T01:00:00Z");
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, null, null, time, time));
        assertInvalid(() -> AuthorizationAuditQuery.of(null, null, null, null, time, time.minusSeconds(1)));
        assertNull(AuthorizationAuditQuery.of(" ", " ", " ", " ", null, null).actorMemberId());
    }

    private static void assertInvalid(Runnable operation) {
        BizException exception = assertThrows(BizException.class, operation::run);
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), exception.getCode());
    }
}
