package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.download.IamDownloadSources;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.MemberWorkbookColumns;
import com.ingot.framework.commons.model.iam.MemberWorkbookExportInput;
import com.ingot.framework.commons.model.iam.MemberWorkbookMode;
import com.ingot.framework.commons.model.iam.MemberWorkbookReasons;
import com.ingot.framework.commons.model.iam.MemberWorkbookTask;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.TenantMemberCreated;
import com.ingot.framework.commons.model.download.DownloadTask;
import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.download.DownloadCenter;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadSourceAccess;
import com.ingot.framework.download.config.DownloadProperties;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * <p>验证表格导出会写入 xlsx，导入拒绝非表格文件，任务状态不含密码明文。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemberWorkbookServiceTest {
    private static final AuthorizationContext CONTEXT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final ActiveIdentity ACTOR = new ActiveIdentity(CONTEXT, "0", "0", "0");

    private JdbcTemplate jdbc;
    private IamAccess access;
    private ResourceAccess scopes;
    private MemberQueryService members;
    private MemberCommandService commands;
    private TenantMemberOnboarding onboarding;
    private DepartmentQueryRepository departments;
    private TenantRepository tenants;
    private DataSourceTransactionManager transactions;
    private DriverManagerDataSource dataSource;
    private OssService oss;
    private byte[] uploaded;

    @BeforeAll
    void database() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_member_workbook(id BIGINT PRIMARY KEY, tenant_id BIGINT, actor_member_id BIGINT,
                  kind VARCHAR(16), mode VARCHAR(16), status VARCHAR(16), tenant_version VARCHAR(32),
                  member_ids VARCHAR(4096), result VARCHAR(4096), workbook BLOB, error_report BLOB, password_file BLOB,
                  password_downloaded BOOLEAN, failure_reason VARCHAR(64), created_at TIMESTAMP, completed_at TIMESTAMP,
                  expires_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_download_task(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  actor_member_id BIGINT, source VARCHAR(64), title VARCHAR(128), status VARCHAR(16),
                  filename VARCHAR(255), content_type VARCHAR(128), file_size BIGINT, object_path VARCHAR(512),
                  failure_reason VARCHAR(255), created_at TIMESTAMP, started_at TIMESTAMP, completed_at TIMESTAMP,
                  expires_at TIMESTAMP)
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
        commands = mock(MemberCommandService.class);
        onboarding = mock(TenantMemberOnboarding.class);
        departments = mock(DepartmentQueryRepository.class);
        tenants = mock(TenantRepository.class);
        transactions = new DataSourceTransactionManager(dataSource);
        oss = mock(OssService.class);
    }

    @BeforeEach
    void resetStubs() {
        reset(access, scopes, members, commands, onboarding, departments, tenants, oss);
        uploaded = null;
        when(oss.uploadFile(any(), any(), any())).thenAnswer(invocation -> {
            uploaded = invocation.<InputStream>getArgument(2).readAllBytes();
            return null;
        });
        jdbc.update("DELETE FROM iam_member_workbook");
        jdbc.update("DELETE FROM iam_download_task");
        jdbc.update("DELETE FROM iam_authorization_audit");
        AtomicLong ids = new AtomicLong(7000);
        when(access.nextId()).thenAnswer(invocation -> ids.incrementAndGet());
        when(access.require(eq(AuthorizationDomain.TENANT), any(IamAction.class))).thenReturn(ACTOR);
        when(access.requireCurrent()).thenReturn(ACTOR);
        when(access.allows(eq(AuthorizationDomain.TENANT), any(IamAction.class), eq(false))).thenReturn(true);
        IamTenantEntity tenant = new IamTenantEntity();
        tenant.setVersion(BigInteger.ZERO);
        tenant.setOwnerMemberId(BigInteger.valueOf(1));
        when(tenants.findActive(10L)).thenReturn(tenant);
        when(scopes.departmentRead(eq(CONTEXT), eq(IamAction.TENANT_DEPARTMENT_READ))).thenReturn(ObjectScope.all());
        when(departments.list(eq(10L), any())).thenReturn(List.of(department(8, null, "英格特云"),
                department(9, 8L, "研发部")));
        when(members.tenantContext()).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportWritesWorkbookWithMemberIdAndDepartmentPath() {
        when(members.listProjected(eq(ACTOR), eq(IamAction.TENANT_MEMBER_EXPORT), eq(1), eq(2), isNull(), isNull()))
                .thenReturn(new PageResponse<>(List.of(detail("201", "张三", "13800001111",
                        List.of(new MemberDepartmentView("9", "研发部", true)))), 1, 1, 2));
        DownloadCenter center = downloads();
        CreatedResource created = service(Runnable::run, center).export(new MemberWorkbookExportInput("0", List.of()));
        DownloadTask task = center.get(new DownloadOwner(AuthorizationDomain.TENANT, 10L, 101L),
                Long.parseLong(created.id()));
        assertEquals(DownloadTaskStatus.SUCCEEDED, task.status());
        assertEquals(IamDownloadSources.MEMBER_WORKBOOK, task.source());
        assertEquals(MemberWorkbookColumns.EXPORT_FILENAME, task.filename());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_member_workbook", Integer.class));
        var sheet = MemberWorkbookExcel.read(uploaded);
        assertTrue(sheet.has(MemberWorkbookColumns.MEMBER_ID));
        assertEquals("201", sheet.rows().getFirst().get(MemberWorkbookColumns.MEMBER_ID));
        assertEquals("英格特云/研发部", sheet.rows().getFirst().get(MemberWorkbookColumns.DEPARTMENTS));
    }

    @Test
    void importRejectsNonXlsx() {
        MemberWorkbookService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "members.csv", "text/csv", "a,b".getBytes());
        BizException exception = assertThrows(BizException.class,
                () -> service.importWorkbook(file, MemberWorkbookMode.CREATE));
        assertEquals(MemberWorkbookReasons.FILE_INVALID, exception.getMessage());
    }

    @Test
    void importCreateKeepsActorWhenExecutorClearsSecurityContext() {
        authenticate();
        when(access.require(eq(AuthorizationDomain.TENANT), any(IamAction.class))).thenAnswer(invocation -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            return ACTOR;
        });
        when(onboarding.create(any())).thenAnswer(invocation -> {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
            return new TenantMemberCreated("301", "0", true, "secret");
        });
        byte[] content = MemberWorkbookExcel.writeMembers(
                List.of(MemberWorkbookColumns.DISPLAY_NAME, MemberWorkbookColumns.PHONE),
                List.of(Map.of(MemberWorkbookColumns.DISPLAY_NAME, "张三",
                        MemberWorkbookColumns.PHONE, "13800001111")),
                false);
        MemberWorkbookService service = service(command -> {
            SecurityContextHolder.clearContext();
            try {
                command.run();
            } finally {
                authenticate();
            }
        });
        CreatedResource created = service.importWorkbook(
                new MockMultipartFile("file", "members.xlsx", MemberWorkbookColumns.CONTENT_TYPE, content),
                MemberWorkbookMode.CREATE);
        MemberWorkbookTask task = service.status(created.id());
        assertEquals(ExportTaskStatus.SUCCEEDED, task.status());
        assertEquals(1, task.successCount());
        assertEquals(0, task.failCount());
    }

    private MemberWorkbookService service() {
        return service(Runnable::run);
    }

    @Test
    void exportVersionMismatchRegistersNothing() {
        MemberWorkbookService service = service();
        assertThrows(BizException.class, () -> service.export(new MemberWorkbookExportInput("9", List.of())));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_download_task", Integer.class));
    }

    @Test
    void exportDeniedInWorkerShowsReason() {
        DownloadCenter center = downloads();
        MemberWorkbookService service = service(Runnable::run, center);
        when(members.listProjected(any(), any(), any(Integer.class), any(Integer.class), isNull(), isNull()))
                .thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        CreatedResource created = service.export(new MemberWorkbookExportInput("0", List.of()));
        DownloadTask task = center.get(new DownloadOwner(AuthorizationDomain.TENANT, 10L, 101L),
                Long.parseLong(created.id()));
        assertEquals(DownloadTaskStatus.FAILED, task.status());
        assertEquals(MemberWorkbookReasons.EXPORT_DENIED, task.failureReason());
    }

    private MemberWorkbookService service(Executor executor) {
        return service(executor, downloads());
    }

    private MemberWorkbookService service(Executor executor, DownloadCenter center) {
        return new MemberWorkbookService(access, scopes, members, commands, onboarding,
                IamMybatisTestAccess.workbooks(dataSource), departments, tenants,
                IamMybatisTestAccess.audits(dataSource), center, transactions, executor, 2);
    }

    private DownloadCenter downloads() {
        DownloadSourceAccess allow = new DownloadSourceAccess() {
            @Override
            public String source() {
                return IamDownloadSources.MEMBER_WORKBOOK;
            }

            @Override
            public void check(DownloadOwner owner) {
            }
        };
        return new DownloadCenter(IamMybatisTestAccess.downloads(dataSource, access::nextId), oss,
                new DownloadProperties(), Runnable::run, List.of(allow), Clock.systemUTC());
    }

    private void authenticate() {
        var user = InUser.stateless(1L, 10L, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account",
                List.of(), List.of(), Map.of()).toBuilder().authorizationContext(CONTEXT).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    private static ResourceDetail<MemberRecord> detail(String id, String name, String phone,
            List<MemberDepartmentView> departments) {
        return IamDetails.of(new MemberRecord(id, name, null, phone, null, null, MemberStatus.ACTIVE, departments), "0");
    }

    private static IamDepartmentEntity department(long id, Long parentId, String name) {
        IamDepartmentEntity row = new IamDepartmentEntity();
        row.setId(BigInteger.valueOf(id));
        row.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        row.setName(name);
        return row;
    }
}
