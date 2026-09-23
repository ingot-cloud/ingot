package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * <p>验证导出遍历全部授权记录、共享库任务状态及下载重验，失败与过期不能报成功。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemberExportServiceTest {
    private static final AuthorizationContext CONTEXT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final ActiveIdentity ACTOR = new ActiveIdentity(CONTEXT, "0", "0", "0");

    private JdbcTemplate jdbc;
    private IamAccess access;
    private ResourceAccess scopes;
    private MemberQueryService members;
    private TenantRepository tenants;
    private DataSourceTransactionManager transactions;
    private DriverManagerDataSource dataSource;

    @BeforeAll
    void database() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_member_export(id BIGINT PRIMARY KEY, tenant_id BIGINT, actor_member_id BIGINT,
                  tenant_version VARCHAR(32), status VARCHAR(16), member_ids VARCHAR(4096), failure_reason VARCHAR(64),
                  created_at TIMESTAMP, completed_at TIMESTAMP, expires_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),actor_account_id BIGINT,
                  actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,target_type VARCHAR(64),target_id VARCHAR(128),
                  change_type VARCHAR(64),safe_before VARCHAR(4096),safe_after VARCHAR(4096),revisions VARCHAR(4096),
                  delegation_id BIGINT,assignment_id BIGINT,trace_id VARCHAR(128),occurred_at TIMESTAMP)
                """);
        access = mock(IamAccess.class);
        scopes = mock(ResourceAccess.class);
        members = mock(MemberQueryService.class);
        tenants = mock(TenantRepository.class);
        transactions = new DataSourceTransactionManager(dataSource);
        when(access.require(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_MEMBER_EXPORT))).thenReturn(ACTOR);
        AtomicLong ids = new AtomicLong(9000);
        when(access.nextId()).thenAnswer(invocation -> ids.incrementAndGet());
        IamTenantEntity tenant = new IamTenantEntity();
        tenant.setVersion(BigInteger.ZERO);
        when(tenants.findActive(10L)).thenReturn(tenant);
        when(scopes.memberRead(eq(CONTEXT), eq(IamAction.TENANT_MEMBER_EXPORT))).thenReturn(ObjectScope.all());
    }

    @BeforeEach
    void resetStubs() {
        reset(members);
        jdbc.update("DELETE FROM iam_member_export");
        jdbc.update("DELETE FROM iam_authorization_audit");
        when(access.require(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_MEMBER_EXPORT))).thenReturn(ACTOR);
        when(scopes.memberRead(eq(CONTEXT), eq(IamAction.TENANT_MEMBER_EXPORT))).thenReturn(ObjectScope.all());
        IamTenantEntity tenant = new IamTenantEntity();
        tenant.setVersion(BigInteger.ZERO);
        when(tenants.findActive(10L)).thenReturn(tenant);
    }

    @Test
    void snapshotWalksAllPagesAndDownloadReturnsCompleteSet() {
        when(members.listProjected(eq(ACTOR), eq(IamAction.TENANT_MEMBER_EXPORT), eq(1), eq(2), isNull(), isNull()))
                .thenReturn(new PageResponse<>(List.of(detail("201"), detail("202")), 3, 1, 2));
        when(members.listProjected(eq(ACTOR), eq(IamAction.TENANT_MEMBER_EXPORT), eq(2), eq(2), isNull(), isNull()))
                .thenReturn(new PageResponse<>(List.of(detail("203")), 3, 2, 2));
        when(members.listProjectedByIds(eq(ACTOR), eq(IamAction.TENANT_MEMBER_EXPORT),
                eq(List.of("201", "202", "203"))))
                .thenReturn(IamPages.complete(List.of(detail("201"), detail("202"), detail("203"))));
        MemberExportService service = service(Runnable::run, 2);
        CreatedResource created = service.create(new VersionInput("0"));
        PageResponse<ResourceDetail<MemberRecord>> downloaded = service.download(created.id());
        assertEquals(3, downloaded.total());
        assertEquals(List.of("201", "202", "203"),
                downloaded.items().stream().map(item -> item.record().id()).toList());
        assertEquals(ExportTaskStatus.SUCCEEDED.getValue(),
                jdbc.queryForObject("SELECT status FROM iam_member_export WHERE id=?", String.class, created.id()));
    }

    @Test
    void pendingTaskCannotBeDownloadedAsSuccess() {
        MemberExportService service = service(command -> { }, 2);
        CreatedResource created = service.create(new VersionInput("0"));
        BizException failure = assertThrows(BizException.class, () -> service.download(created.id()));
        assertEquals(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(), failure.getCode());
    }

    @Test
    void failedSnapshotIsNotDownloadable() {
        when(members.listProjected(any(), any(), any(Integer.class), any(Integer.class), any(), any()))
                .thenThrow(new IllegalStateException("boom"));
        MemberExportService service = service(Runnable::run, 2);
        CreatedResource created = service.create(new VersionInput("0"));
        BizException failure = assertThrows(BizException.class, () -> service.download(created.id()));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), failure.getCode());
        assertEquals(ExportTaskStatus.FAILED.getValue(),
                jdbc.queryForObject("SELECT status FROM iam_member_export WHERE id=?", String.class, created.id()));
    }

    @Test
    void expiredSnapshotIsNotDownloadable() {
        jdbc.update("""
                INSERT INTO iam_member_export(id, tenant_id, actor_member_id, tenant_version, status, member_ids,
                  created_at, expires_at) VALUES (81,10,101,'0','SUCCEEDED','["201"]',CURRENT_TIMESTAMP,?)
                """, LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
        MemberExportService service = service(Runnable::run, 2);
        BizException failure = assertThrows(BizException.class, () -> service.download("81"));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), failure.getCode());
        assertEquals(ExportTaskStatus.EXPIRED.getValue(),
                jdbc.queryForObject("SELECT status FROM iam_member_export WHERE id=81", String.class));
    }

    @Test
    void statusReturnsLifecycleWithoutMemberSnapshot() {
        MemberExportService service = service(command -> { }, 2);
        CreatedResource created = service.create(new VersionInput("0"));
        var pending = service.status(created.id());
        assertEquals(ExportTaskStatus.PENDING, pending.status());
        assertEquals("0", pending.version());
        assertEquals(null, pending.failureCode());
        jdbc.update("UPDATE iam_member_export SET status='FAILED', failure_reason='SNAPSHOT_FAILED' WHERE id=?",
                created.id());
        var failed = service.status(created.id());
        assertEquals(ExportTaskStatus.FAILED, failed.status());
        assertEquals("SNAPSHOT_FAILED", failed.failureCode());
    }

    private MemberExportService service(java.util.concurrent.Executor executor, int pageSize) {
        return new MemberExportService(access, scopes, members, IamMybatisTestAccess.exports(dataSource),
                IamMybatisTestAccess.audits(dataSource), tenants, transactions, executor, pageSize);
    }

    private static ResourceDetail<MemberRecord> detail(String id) {
        return IamDetails.of(new MemberRecord(id, "成员", null, null, null, null, MemberStatus.ACTIVE, List.of()), "0");
    }
}
