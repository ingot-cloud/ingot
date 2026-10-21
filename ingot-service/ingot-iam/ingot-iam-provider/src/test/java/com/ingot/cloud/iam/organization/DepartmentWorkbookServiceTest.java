package com.ingot.cloud.iam.organization;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.download.IamDownloadSources;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.DepartmentWorkbookRepository;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentWorkbookEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.download.DownloadTask;
import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DepartmentDraft;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookColumns;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookExportInput;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookReasons;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberWorkbookArtifact;
import com.ingot.framework.commons.model.iam.MemberWorkbookKind;
import com.ingot.framework.commons.model.iam.MemberWorkbookMode;
import com.ingot.framework.commons.model.iam.MemberWorkbookTask;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * <p>验证部门表格导出写入 xlsx，导入逐行独立失败，他人任务不可见。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepartmentWorkbookServiceTest {
    private static final AuthorizationContext CONTEXT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final ActiveIdentity ACTOR = new ActiveIdentity(CONTEXT, "0", "0", "0");

    private JdbcTemplate jdbc;
    private IamAccess access;
    private ResourceAccess scopes;
    private DepartmentService departments;
    private DepartmentQueryRepository departmentRows;
    private TenantRepository tenants;
    private DataSourceTransactionManager transactions;
    private DriverManagerDataSource dataSource;
    private OssService oss;
    private final Map<String, byte[]> objects = new LinkedHashMap<>();
    private byte[] uploaded;

    @BeforeAll
    void database() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_department_workbook(id BIGINT PRIMARY KEY, tenant_id BIGINT, actor_member_id BIGINT,
                  kind VARCHAR(16), mode VARCHAR(16), status VARCHAR(16), tenant_version VARCHAR(32),
                  department_ids VARCHAR(4096), result VARCHAR(4096), workbook_path VARCHAR(512),
                  error_report_path VARCHAR(512), failure_reason VARCHAR(64), created_at TIMESTAMP,
                  completed_at TIMESTAMP, expires_at TIMESTAMP)
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
        departments = mock(DepartmentService.class);
        departmentRows = mock(DepartmentQueryRepository.class);
        tenants = mock(TenantRepository.class);
        transactions = new DataSourceTransactionManager(dataSource);
        oss = mock(OssService.class);
    }

    @BeforeEach
    void resetStubs() {
        reset(access, scopes, departments, departmentRows, tenants, oss);
        objects.clear();
        uploaded = null;
        when(oss.uploadFile(any(), any(), any())).thenAnswer(invocation -> {
            uploaded = invocation.<InputStream>getArgument(2).readAllBytes();
            objects.put(invocation.getArgument(1), uploaded);
            return null;
        });
        when(oss.getObjectBytes(any(), any())).thenAnswer(invocation -> objects.get(invocation.getArgument(1)));
        jdbc.update("DELETE FROM iam_department_workbook");
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
        when(departmentRows.list(eq(10L), any())).thenReturn(List.of(
                department(8, null, "英格特云", 0),
                department(9, 8L, "研发部", 1),
                department(10, 9L, "后端组", 1)));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void templateOmitsDepartmentId() {
        var sheet = DepartmentWorkbookExcel.read(decode(service().template().contentBase64()));
        assertTrue(sheet.has(DepartmentWorkbookColumns.NAME));
        assertTrue(sheet.has(DepartmentWorkbookColumns.PARENT));
        assertTrue(sheet.has(DepartmentWorkbookColumns.SORT_ORDER));
        assertTrue(!sheet.has(DepartmentWorkbookColumns.DEPARTMENT_ID));
        assertEquals(0, sheet.rows().size());
    }

    @Test
    void exportWritesWorkbookInTreeOrder() {
        DownloadCenter center = downloads();
        CreatedResource created = service(Runnable::run, center)
                .export(new DepartmentWorkbookExportInput("0", List.of()));
        DownloadTask task = center.get(new DownloadOwner(AuthorizationDomain.TENANT, 10L, 101L),
                Long.parseLong(created.id()));
        assertEquals(DownloadTaskStatus.SUCCEEDED, task.status());
        assertEquals(IamDownloadSources.DEPARTMENT_WORKBOOK, task.source());
        assertEquals(DepartmentWorkbookColumns.EXPORT_FILENAME, task.filename());
        var sheet = DepartmentWorkbookExcel.read(uploaded);
        assertTrue(sheet.has(DepartmentWorkbookColumns.DEPARTMENT_ID));
        assertEquals(List.of("8", "9", "10"), sheet.rows().stream()
                .map(row -> row.get(DepartmentWorkbookColumns.DEPARTMENT_ID)).toList());
        assertEquals("英格特云", sheet.rows().get(1).get(DepartmentWorkbookColumns.PARENT));
        assertEquals("英格特云/研发部", sheet.rows().get(2).get(DepartmentWorkbookColumns.PARENT));
    }

    @Test
    void exportPartialExpandsDescendants() {
        DownloadCenter center = downloads();
        service(Runnable::run, center).export(new DepartmentWorkbookExportInput("0", List.of("9")));
        var sheet = DepartmentWorkbookExcel.read(uploaded);
        assertEquals(List.of("9", "10"), sheet.rows().stream()
                .map(row -> row.get(DepartmentWorkbookColumns.DEPARTMENT_ID)).toList());
    }

    @Test
    void exportDeniedInWorkerShowsReason() {
        DownloadCenter center = downloads();
        AtomicInteger calls = new AtomicInteger();
        when(access.require(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_DEPARTMENT_EXPORT)))
                .thenAnswer(invocation -> {
                    if (calls.incrementAndGet() > 1) {
                        throw new BizException(IamReasonCode.ACTION_DENIED);
                    }
                    return ACTOR;
                });
        CreatedResource created = service(Runnable::run, center)
                .export(new DepartmentWorkbookExportInput("0", List.of()));
        DownloadTask task = center.get(new DownloadOwner(AuthorizationDomain.TENANT, 10L, 101L),
                Long.parseLong(created.id()));
        assertEquals(DownloadTaskStatus.FAILED, task.status());
        assertEquals(DepartmentWorkbookReasons.EXPORT_DENIED, task.failureReason());
    }

    @Test
    void importRejectsNonXlsx() {
        MockMultipartFile file = new MockMultipartFile("file", "departments.csv", "text/csv", "a,b".getBytes());
        BizException exception = assertThrows(BizException.class,
                () -> service().importWorkbook(file, MemberWorkbookMode.CREATE));
        assertEquals(DepartmentWorkbookReasons.FILE_INVALID, exception.getMessage());
    }

    @Test
    void importCreateRegistersPriorRowAsParentAndCountsPartialFailure() {
        AtomicLong createdIds = new AtomicLong(20);
        when(departments.create(any())).thenAnswer(invocation -> {
            DepartmentDraft draft = invocation.getArgument(0);
            if ("失败部".equals(draft.name())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "写入失败");
            }
            return new CreatedResource(Long.toString(createdIds.incrementAndGet()), "0");
        });
        byte[] content = DepartmentWorkbookExcel.writeDepartments(
                List.of(DepartmentWorkbookColumns.NAME, DepartmentWorkbookColumns.PARENT,
                        DepartmentWorkbookColumns.SORT_ORDER),
                List.of(
                        Map.of(DepartmentWorkbookColumns.NAME, "新产品",
                                DepartmentWorkbookColumns.PARENT, "英格特云"),
                        Map.of(DepartmentWorkbookColumns.NAME, "设计组",
                                DepartmentWorkbookColumns.PARENT, "英格特云/新产品"),
                        Map.of(DepartmentWorkbookColumns.NAME, "失败部",
                                DepartmentWorkbookColumns.PARENT, "英格特云")),
                false);
        CreatedResource created = service().importWorkbook(file(content), MemberWorkbookMode.CREATE);
        MemberWorkbookTask task = service().status(created.id());
        assertEquals(ExportTaskStatus.SUCCEEDED, task.status());
        assertEquals(2, task.successCount());
        assertEquals(1, task.failCount());
    }

    @Test
    void importCreateRejectsIdAndRootAndDuplicateSibling() {
        when(departments.create(any())).thenReturn(new CreatedResource("21", "0"));
        byte[] content = DepartmentWorkbookExcel.writeDepartments(
                List.of(DepartmentWorkbookColumns.DEPARTMENT_ID, DepartmentWorkbookColumns.NAME,
                        DepartmentWorkbookColumns.PARENT),
                List.of(
                        Map.of(DepartmentWorkbookColumns.DEPARTMENT_ID, "99",
                                DepartmentWorkbookColumns.NAME, "新产品",
                                DepartmentWorkbookColumns.PARENT, "英格特云"),
                        Map.of(DepartmentWorkbookColumns.NAME, "根",
                                DepartmentWorkbookColumns.PARENT, ""),
                        Map.of(DepartmentWorkbookColumns.NAME, "研发部",
                                DepartmentWorkbookColumns.PARENT, "英格特云")),
                false);
        CreatedResource created = service().importWorkbook(file(content), MemberWorkbookMode.CREATE);
        MemberWorkbookTask task = service().status(created.id());
        assertEquals(0, task.successCount());
        assertEquals(3, task.failCount());
        assertEquals(List.of(DepartmentWorkbookReasons.ID_ON_CREATE, DepartmentWorkbookReasons.ROOT_CREATE,
                        DepartmentWorkbookReasons.NAME_DUPLICATE),
                errorReasons(created.id()));
    }

    @Test
    void importUpdateRejectsRootParentAndCycle() {
        when(departments.get("8")).thenReturn(IamDetails.of(
                new DepartmentRecord("8", null, "英格特云", 0, false, 0), "0"));
        when(departments.get("9")).thenReturn(IamDetails.of(
                new DepartmentRecord("9", "8", "研发部", 1, false, 0), "0"));
        byte[] content = DepartmentWorkbookExcel.writeDepartments(
                List.of(DepartmentWorkbookColumns.DEPARTMENT_ID, DepartmentWorkbookColumns.NAME,
                        DepartmentWorkbookColumns.PARENT),
                List.of(
                        Map.of(DepartmentWorkbookColumns.DEPARTMENT_ID, "8",
                                DepartmentWorkbookColumns.NAME, "英格特云",
                                DepartmentWorkbookColumns.PARENT, "英格特云/研发部"),
                        Map.of(DepartmentWorkbookColumns.DEPARTMENT_ID, "9",
                                DepartmentWorkbookColumns.NAME, "研发部",
                                DepartmentWorkbookColumns.PARENT, "英格特云/研发部/后端组"),
                        Map.of(DepartmentWorkbookColumns.DEPARTMENT_ID, "8",
                                DepartmentWorkbookColumns.NAME, "集团",
                                DepartmentWorkbookColumns.PARENT, "")),
                true);
        CreatedResource created = service().importWorkbook(file(content), MemberWorkbookMode.UPDATE);
        MemberWorkbookTask task = service().status(created.id());
        assertEquals(1, task.successCount());
        assertEquals(2, task.failCount());
        assertEquals(List.of(DepartmentWorkbookReasons.ROOT_PARENT, DepartmentWorkbookReasons.PARENT_CYCLE),
                errorReasons(created.id()));
    }

    @Test
    void otherActorTaskIsNotFound() {
        DepartmentWorkbookRepository repository = IamMybatisTestAccess.departmentWorkbooks(dataSource);
        IamDepartmentWorkbookEntity row = new IamDepartmentWorkbookEntity();
        row.setId(BigInteger.valueOf(88));
        row.setTenantId(BigInteger.valueOf(10));
        row.setActorMemberId(BigInteger.valueOf(999));
        row.setKind(MemberWorkbookKind.IMPORT);
        row.setMode(MemberWorkbookMode.CREATE);
        row.setStatus(ExportTaskStatus.SUCCEEDED);
        row.setTenantVersion("0");
        row.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        row.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));
        repository.insert(row);
        BizException exception = assertThrows(BizException.class, () -> service().status("88"));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void importKeepsActorWhenExecutorClearsSecurityContext() {
        authenticate();
        when(access.require(eq(AuthorizationDomain.TENANT), any(IamAction.class))).thenAnswer(invocation -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            return ACTOR;
        });
        when(departments.create(any())).thenAnswer(invocation -> {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_CREATE);
            return new CreatedResource("21", "0");
        });
        byte[] content = DepartmentWorkbookExcel.writeDepartments(
                List.of(DepartmentWorkbookColumns.NAME, DepartmentWorkbookColumns.PARENT),
                List.of(Map.of(DepartmentWorkbookColumns.NAME, "新产品",
                        DepartmentWorkbookColumns.PARENT, "英格特云")),
                false);
        CreatedResource created = service(command -> {
            SecurityContextHolder.clearContext();
            try {
                command.run();
            } finally {
                authenticate();
            }
        }).importWorkbook(file(content), MemberWorkbookMode.CREATE);
        MemberWorkbookTask task = service().status(created.id());
        assertEquals(ExportTaskStatus.SUCCEEDED, task.status());
        assertEquals(1, task.successCount());
    }

    private DepartmentWorkbookService service() {
        return service(Runnable::run);
    }

    private DepartmentWorkbookService service(Executor executor) {
        return service(executor, downloads());
    }

    private DepartmentWorkbookService service(Executor executor, DownloadCenter center) {
        return new DepartmentWorkbookService(access, scopes, departments,
                IamMybatisTestAccess.departmentWorkbooks(dataSource), departmentRows, tenants,
                IamMybatisTestAccess.audits(dataSource), center, oss, new DownloadProperties(),
                transactions, executor);
    }

    private DownloadCenter downloads() {
        DownloadSourceAccess allow = new DownloadSourceAccess() {
            @Override
            public String source() {
                return IamDownloadSources.DEPARTMENT_WORKBOOK;
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

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "departments.xlsx", DepartmentWorkbookColumns.CONTENT_TYPE, content);
    }

    private static byte[] decode(String contentBase64) {
        return java.util.Base64.getDecoder().decode(contentBase64);
    }

    private List<String> errorReasons(String workbookId) {
        byte[] content = decode(service().file(workbookId, MemberWorkbookArtifact.ERRORS).contentBase64());
        List<Map<Integer, String>> rows = new java.util.ArrayList<>();
        com.alibaba.excel.EasyExcel.read(new java.io.ByteArrayInputStream(content),
                new com.alibaba.excel.event.AnalysisEventListener<Map<Integer, String>>() {
                    @Override
                    public void invoke(Map<Integer, String> data, com.alibaba.excel.context.AnalysisContext context) {
                        rows.add(data);
                    }

                    @Override
                    public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                    }
                }).headRowNumber(0).sheet().doRead();
        return rows.stream().skip(1).map(row -> DepartmentWorkbookExcel.normalize(row.get(1))).toList();
    }

    private static IamDepartmentEntity department(long id, Long parentId, String name, int sortOrder) {
        IamDepartmentEntity row = new IamDepartmentEntity();
        row.setId(BigInteger.valueOf(id));
        row.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        row.setName(name);
        row.setSortOrder(sortOrder);
        return row;
    }
}
