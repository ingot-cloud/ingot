package com.ingot.cloud.iam.organization;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.download.IamDownloadOwners;
import com.ingot.cloud.iam.download.IamDownloadSources;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.organization.DepartmentWorkbookExcel.DepartmentWorkbookOutcome;
import com.ingot.cloud.iam.organization.DepartmentWorkbookExcel.DepartmentWorkbookRowFailure;
import com.ingot.cloud.iam.organization.DepartmentWorkbookExcel.DepartmentWorkbookSheet;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.DepartmentWorkbookRepository;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentWorkbookEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DepartmentDraft;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentUpdateInput;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookColumns;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookExportInput;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookLimits;
import com.ingot.framework.commons.model.iam.DepartmentWorkbookReasons;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberWorkbookArtifact;
import com.ingot.framework.commons.model.iam.MemberWorkbookFile;
import com.ingot.framework.commons.model.iam.MemberWorkbookHistoryItem;
import com.ingot.framework.commons.model.iam.MemberWorkbookKind;
import com.ingot.framework.commons.model.iam.MemberWorkbookMode;
import com.ingot.framework.commons.model.iam.MemberWorkbookTask;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.download.DownloadCenter;
import com.ingot.framework.download.DownloadFile;
import com.ingot.framework.download.DownloadJobException;
import com.ingot.framework.download.DownloadRequest;
import com.ingot.framework.download.config.DownloadProperties;
import com.ingot.framework.oss.common.OssObjectInfo;
import com.ingot.framework.oss.common.OssPathParser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>登记部门表格导入任务并逐行复用部门创建与更新；导出交给下载中心生成。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class DepartmentWorkbookService {
    private static final String TARGET = "department-workbook";
    private static final String OBJECT_PREFIX = "department-workbook";
    private static final TypeReference<DepartmentWorkbookOutcome> OUTCOME_TYPE = new TypeReference<>() {
    };
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final DepartmentService departments;
    private final DepartmentWorkbookRepository workbooks;
    private final DepartmentQueryRepository departmentRows;
    private final TenantRepository tenants;
    private final IamAuditWriter audits;
    private final DownloadCenter downloads;
    private final OssService oss;
    private final DownloadProperties downloadProperties;
    private final TransactionTemplate transaction;
    private final Executor executor;

    /**
     * 绑定身份、部门写入、任务存储与对象存储。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成；执行器需排除调度器，
     * 并在提交时绑定当前认证。保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param departments 部门写入
     * @param workbooks 任务存储
     * @param departmentRows 部门目录
     * @param tenants 组织版本
     * @param audits 同事务审计
     * @param downloads 下载中心
     * @param oss 对象存储
     * @param downloadProperties 下载桶
     * @param transactionManager 同一数据源事务
     * @param executors 应用异步执行器
     */
    @Autowired
    public DepartmentWorkbookService(IamAccess access, ResourceAccess scopes, DepartmentService departments,
            DepartmentWorkbookRepository workbooks, DepartmentQueryRepository departmentRows, TenantRepository tenants,
            IamAuditWriter audits, DownloadCenter downloads, OssService oss, DownloadProperties downloadProperties,
            PlatformTransactionManager transactionManager, ObjectProvider<TaskExecutor> executors) {
        this(access, scopes, departments, workbooks, departmentRows, tenants, audits, downloads, oss, downloadProperties,
                transactionManager, applicationExecutor(executors));
    }

    /**
     * 指定执行器，供测试同步跑完导入。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param departments 部门写入
     * @param workbooks 任务存储
     * @param departmentRows 部门目录
     * @param tenants 组织版本
     * @param audits 同事务审计
     * @param downloads 下载中心
     * @param oss 对象存储
     * @param downloadProperties 下载桶
     * @param transactionManager 同一数据源事务
     * @param executor 导入任务执行器
     */
    public DepartmentWorkbookService(IamAccess access, ResourceAccess scopes, DepartmentService departments,
            DepartmentWorkbookRepository workbooks, DepartmentQueryRepository departmentRows, TenantRepository tenants,
            IamAuditWriter audits, DownloadCenter downloads, OssService oss, DownloadProperties downloadProperties,
            PlatformTransactionManager transactionManager, Executor executor) {
        this.access = access;
        this.scopes = scopes;
        this.departments = departments;
        this.workbooks = workbooks;
        this.departmentRows = departmentRows;
        this.tenants = tenants;
        this.audits = audits;
        this.downloads = downloads;
        this.oss = oss;
        this.downloadProperties = downloadProperties;
        this.transaction = new TransactionTemplate(transactionManager);
        this.executor = new DelegatingSecurityContextExecutor(executor == null ? Runnable::run : executor);
    }

    private static TaskExecutor applicationExecutor(ObjectProvider<TaskExecutor> executors) {
        if (executors == null) {
            return null;
        }
        return executors.orderedStream()
                .filter(candidate -> !(candidate instanceof TaskScheduler))
                .findFirst()
                .orElse(null);
    }

    /**
     * 生成空白创建模板。
     *
     * @return 模板文件
     */
    public MemberWorkbookFile template() {
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_CREATE);
        return fileOf(DepartmentWorkbookColumns.TEMPLATE_FILENAME,
                DepartmentWorkbookExcel.writeDepartments(createHeaders(), List.of(), false));
    }

    /**
     * 校验组织版本后向下载中心登记更新导出，结果在「我的下载」查看。
     *
     * @param input 组织版本与可选部门范围
     * @return 下载任务 ID 与组织版本
     */
    public CreatedResource export(DepartmentWorkbookExportInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_EXPORT);
        List<String> departmentIds = input.departmentIds() == null ? List.of() : List.copyOf(input.departmentIds());
        DownloadRequest request = new DownloadRequest(IamDownloadOwners.of(actor),
                IamDownloadSources.DEPARTMENT_WORKBOOK, IamDownloadSources.DEPARTMENT_WORKBOOK_TITLE);
        CreatedResource created = transaction.execute(status -> {
            String version = tenantVersion(IamIds.require(actor.context().tenantId()));
            IamIds.requireVersion(input.expectedVersion(), version);
            long id = downloads.submit(request, () -> exportFile(actor, departmentIds));
            audits.write(actor.context(), access.nextId(), TARGET, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(), Map.of("tenant", version));
            return new CreatedResource(IamIds.text(id), version);
        });
        if (created == null) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        return created;
    }

    /**
     * 校验文件后登记导入任务，提交后再异步逐行写入。
     *
     * @param file xlsx
     * @param mode 创建或更新
     * @return 任务 ID
     */
    public CreatedResource importWorkbook(MultipartFile file, MemberWorkbookMode mode) {
        if (mode == null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        ActiveIdentity actor = requireImport(mode);
        byte[] content = readUpload(file);
        DepartmentWorkbookExcel.read(content);
        return register(actor, mode, content);
    }

    /**
     * 返回当前操作者最近导入摘要。
     *
     * @return 最多十条
     */
    public List<MemberWorkbookHistoryItem> history() {
        ActiveIdentity actor = requireHistory();
        long tenantId = IamIds.require(actor.context().tenantId());
        long actorId = IamIds.require(actor.context().memberId());
        expireDue(tenantId);
        List<MemberWorkbookHistoryItem> items = new ArrayList<>();
        for (IamDepartmentWorkbookEntity row : workbooks.listRecentImports(tenantId, actorId)) {
            DepartmentWorkbookOutcome outcome = outcomeOf(row);
            items.add(new MemberWorkbookHistoryItem(IamIds.text(row.getId().longValueExact()), row.getKind(),
                    row.getMode(), row.getStatus(), row.getCreatedAt().toInstant(ZoneOffset.UTC),
                    row.getExpiresAt().toInstant(ZoneOffset.UTC),
                    outcome == null ? null : outcome.successCount(),
                    outcome == null ? null : outcome.failCount()));
        }
        return List.copyOf(items);
    }

    /**
     * 返回任务状态，进行中也可读取。
     *
     * @param workbookId 任务 ID
     * @return 状态
     */
    public MemberWorkbookTask status(String workbookId) {
        IamDepartmentWorkbookEntity row = requireOwned(workbookId);
        DepartmentWorkbookOutcome outcome = outcomeOf(row);
        return new MemberWorkbookTask(IamIds.text(row.getId().longValueExact()), row.getKind(), row.getMode(),
                row.getStatus(), row.getTenantVersion(), row.getCreatedAt().toInstant(ZoneOffset.UTC),
                row.getExpiresAt().toInstant(ZoneOffset.UTC),
                row.getStatus() == ExportTaskStatus.FAILED ? row.getFailureReason() : null,
                outcome == null ? null : outcome.successCount(),
                outcome == null ? null : outcome.failCount(),
                null);
    }

    /**
     * 下载导入任务产物。
     *
     * @param workbookId 任务 ID
     * @param artifact 产物
     * @return 文件
     */
    public MemberWorkbookFile file(String workbookId, MemberWorkbookArtifact artifact) {
        if (artifact == null || artifact == MemberWorkbookArtifact.PASSWORDS) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        IamDepartmentWorkbookEntity row = requireOwned(workbookId);
        if (row.getStatus() == ExportTaskStatus.PENDING || row.getStatus() == ExportTaskStatus.RUNNING) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        if (row.getStatus() != ExportTaskStatus.SUCCEEDED) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        String path = artifact == MemberWorkbookArtifact.ERRORS ? row.getErrorReportPath() : row.getWorkbookPath();
        byte[] content = readObject(path);
        if (content == null || content.length == 0) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        String name = artifact == MemberWorkbookArtifact.ERRORS ? DepartmentWorkbookColumns.ERROR_FILENAME
                : DepartmentWorkbookColumns.TEMPLATE_FILENAME;
        return fileOf(name, content);
    }

    private CreatedResource register(ActiveIdentity actor, MemberWorkbookMode mode, byte[] workbook) {
        long tenantId = IamIds.require(actor.context().tenantId());
        long actorId = IamIds.require(actor.context().memberId());
        CreatedResource created = transaction.execute(status -> {
            expireDue(tenantId);
            String version = tenantVersion(tenantId);
            long id = access.nextId();
            String workbookPath = storeObject(tenantId, actorId, id, DepartmentWorkbookColumns.TEMPLATE_FILENAME, workbook);
            IamDepartmentWorkbookEntity row = new IamDepartmentWorkbookEntity();
            row.setId(BigInteger.valueOf(id));
            row.setTenantId(BigInteger.valueOf(tenantId));
            row.setActorMemberId(BigInteger.valueOf(actorId));
            row.setKind(MemberWorkbookKind.IMPORT);
            row.setMode(mode);
            row.setStatus(ExportTaskStatus.PENDING);
            row.setTenantVersion(version);
            row.setWorkbookPath(workbookPath);
            row.setCreatedAt(utcNow());
            row.setExpiresAt(utcNow().plus(DepartmentWorkbookLimits.RETENTION));
            workbooks.insert(row);
            audits.write(actor.context(), access.nextId(), TARGET, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(), Map.of("tenant", version));
            return new CreatedResource(IamIds.text(id), version);
        });
        if (created == null) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        executor.execute(() -> process(actor, IamIds.require(created.id())));
        return created;
    }

    private void process(ActiveIdentity actor, long workbookId) {
        long tenantId = IamIds.require(actor.context().tenantId());
        if (!workbooks.markRunning(tenantId, workbookId)) {
            return;
        }
        try {
            IamDepartmentWorkbookEntity row = workbooks.findOwned(tenantId, IamIds.require(actor.context().memberId()),
                    workbookId);
            if (row == null || row.getKind() != MemberWorkbookKind.IMPORT) {
                workbooks.markFailed(tenantId, workbookId, DepartmentWorkbookReasons.FAILURE_PROCESS, utcNow());
                return;
            }
            completeImport(actor, row);
        } catch (RuntimeException exception) {
            workbooks.markFailed(tenantId, workbookId, failureReason(exception), utcNow());
        }
    }

    private DownloadFile exportFile(ActiveIdentity actor, List<String> departmentIds) {
        try {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_EXPORT);
            DepartmentWorkbookCatalog catalog = catalog(actor);
            List<IamDepartmentEntity> rows = catalog.exportRows(departmentIds);
            List<String> headers = updateHeaders();
            List<Map<String, String>> data = new ArrayList<>();
            for (IamDepartmentEntity row : rows) {
                Map<String, String> values = new LinkedHashMap<>();
                String id = IamIds.text(row.getId().longValueExact());
                values.put(DepartmentWorkbookColumns.DEPARTMENT_ID, id);
                values.put(DepartmentWorkbookColumns.NAME, text(row.getName()));
                String parentPath = "";
                if (row.getParentId() != null) {
                    parentPath = catalog.format(IamIds.text(row.getParentId().longValueExact()));
                }
                values.put(DepartmentWorkbookColumns.PARENT, parentPath);
                values.put(DepartmentWorkbookColumns.SORT_ORDER,
                        row.getSortOrder() == null ? "0" : Integer.toString(row.getSortOrder()));
                data.add(values);
            }
            return new DownloadFile(DepartmentWorkbookColumns.EXPORT_FILENAME, DepartmentWorkbookColumns.CONTENT_TYPE,
                    DepartmentWorkbookExcel.writeDepartments(headers, data, true));
        } catch (BizException exception) {
            throw new DownloadJobException(exportFailure(exception));
        }
    }

    private void completeImport(ActiveIdentity actor, IamDepartmentWorkbookEntity row) {
        byte[] workbook = readObject(row.getWorkbookPath());
        DepartmentWorkbookSheet sheet = DepartmentWorkbookExcel.read(workbook);
        DepartmentWorkbookCatalog catalog = catalog(actor);
        List<DepartmentWorkbookRowFailure> errors = new ArrayList<>();
        int success = 0;
        for (int index = 0; index < sheet.rows().size(); index++) {
            int excelRow = sheet.excelRow(index);
            try {
                if (row.getMode() == MemberWorkbookMode.CREATE) {
                    importCreate(sheet.rows().get(index), sheet, catalog);
                } else {
                    importUpdate(sheet.rows().get(index), sheet, catalog);
                }
                success++;
            } catch (BizException exception) {
                errors.add(new DepartmentWorkbookRowFailure(excelRow, rowReason(exception)));
            }
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long actorId = IamIds.require(actor.context().memberId());
        long workbookId = row.getId().longValueExact();
        String errorPath = null;
        if (!errors.isEmpty()) {
            errorPath = storeObject(tenantId, actorId, workbookId, DepartmentWorkbookColumns.ERROR_FILENAME,
                    DepartmentWorkbookExcel.writeErrors(errors));
        }
        if (!workbooks.markSucceeded(tenantId, workbookId,
                IamJson.object(new DepartmentWorkbookOutcome(success, errors.size(), errors)),
                row.getWorkbookPath(), errorPath, utcNow())) {
            workbooks.markFailed(tenantId, workbookId, DepartmentWorkbookReasons.FAILURE_PROCESS, utcNow());
        }
    }

    private void importCreate(Map<String, String> row, DepartmentWorkbookSheet sheet,
            DepartmentWorkbookCatalog catalog) {
        if (sheet.has(DepartmentWorkbookColumns.DEPARTMENT_ID)
                && !text(row.get(DepartmentWorkbookColumns.DEPARTMENT_ID)).isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.ID_ON_CREATE);
        }
        String name = requireName(row, sheet, true);
        String parentId = requireParentId(row, sheet, catalog, true, false);
        if (catalog.siblingTaken(parentId, name, null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.NAME_DUPLICATE);
        }
        int sortOrder = parseSort(row, sheet);
        CreatedResource created = departments.create(new DepartmentDraft(parentId, name, sortOrder));
        IamDepartmentEntity stored = new IamDepartmentEntity();
        stored.setId(BigInteger.valueOf(IamIds.require(created.id())));
        stored.setParentId(parentId == null ? null : BigInteger.valueOf(IamIds.require(parentId)));
        stored.setName(name);
        stored.setSortOrder(sortOrder);
        catalog.register(stored);
    }

    private void importUpdate(Map<String, String> row, DepartmentWorkbookSheet sheet,
            DepartmentWorkbookCatalog catalog) {
        String departmentId = text(row.get(DepartmentWorkbookColumns.DEPARTMENT_ID));
        if (departmentId.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(),
                    DepartmentWorkbookReasons.DEPARTMENT_ID_REQUIRED);
        }
        ResourceDetail<DepartmentRecord> current;
        try {
            current = departments.get(departmentId);
        } catch (BizException exception) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND.getCode(),
                    DepartmentWorkbookReasons.DEPARTMENT_NOT_FOUND);
        }
        boolean root = current.record().parentId() == null || current.record().parentId().isBlank();
        String name = sheet.has(DepartmentWorkbookColumns.NAME)
                ? requireName(row, sheet, true) : current.record().name();
        String parentId = current.record().parentId();
        if (sheet.has(DepartmentWorkbookColumns.PARENT)) {
            parentId = requireParentId(row, sheet, catalog, false, root);
        }
        if (catalog.wouldCycle(departmentId, parentId)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.PARENT_CYCLE);
        }
        if (catalog.siblingTaken(parentId, name, departmentId)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.NAME_DUPLICATE);
        }
        int sortOrder = sheet.has(DepartmentWorkbookColumns.SORT_ORDER)
                ? parseSort(row, sheet) : current.record().sortOrder();
        departments.update(departmentId, new DepartmentUpdateInput(current.version(),
                new DepartmentDraft(parentId, name, sortOrder)));
        IamDepartmentEntity stored = catalog.get(departmentId);
        if (stored != null) {
            stored.setName(name);
            stored.setParentId(parentId == null || parentId.isBlank() ? null : BigInteger.valueOf(IamIds.require(parentId)));
            stored.setSortOrder(sortOrder);
            catalog.refresh();
        }
    }

    private String requireName(Map<String, String> row, DepartmentWorkbookSheet sheet, boolean required) {
        String name = text(row.get(DepartmentWorkbookColumns.NAME));
        if (required && sheet.has(DepartmentWorkbookColumns.NAME) && name.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.NAME_REQUIRED);
        }
        if (name.contains(DepartmentWorkbookColumns.PATH_SEPARATOR)
                || name.length() > DepartmentWorkbookLimits.NAME_MAX_LENGTH) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.NAME_INVALID);
        }
        return name;
    }

    private String requireParentId(Map<String, String> row, DepartmentWorkbookSheet sheet,
            DepartmentWorkbookCatalog catalog, boolean create, boolean root) {
        String path = text(row.get(DepartmentWorkbookColumns.PARENT));
        if (!sheet.has(DepartmentWorkbookColumns.PARENT) || path.isBlank()) {
            if (create) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.ROOT_CREATE);
            }
            if (root) {
                return null;
            }
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.PARENT_REQUIRED);
        }
        if (root) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.ROOT_PARENT);
        }
        return catalog.requireParent(path);
    }

    private int parseSort(Map<String, String> row, DepartmentWorkbookSheet sheet) {
        if (!sheet.has(DepartmentWorkbookColumns.SORT_ORDER)) {
            return 0;
        }
        String value = text(row.get(DepartmentWorkbookColumns.SORT_ORDER));
        if (value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.SORT_INVALID);
        }
    }

    private DepartmentWorkbookCatalog catalog(ActiveIdentity actor) {
        long tenantId = IamIds.require(actor.context().tenantId());
        ObjectScope scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
        return new DepartmentWorkbookCatalog(departmentRows.list(tenantId, scope));
    }

    private IamDepartmentWorkbookEntity requireOwned(String workbookId) {
        ActiveIdentity actor = access.requireCurrent();
        long tenantId = IamIds.require(actor.context().tenantId());
        long actorId = IamIds.require(actor.context().memberId());
        expireDue(tenantId);
        IamDepartmentWorkbookEntity row = workbooks.findOwned(tenantId, actorId, IamIds.require(workbookId));
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        requireImport(row.getMode());
        return row;
    }

    private ActiveIdentity requireImport(MemberWorkbookMode mode) {
        if (mode == MemberWorkbookMode.CREATE) {
            return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_CREATE);
        }
        return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_UPDATE);
    }

    private ActiveIdentity requireHistory() {
        if (access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_CREATE, false)) {
            return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_CREATE);
        }
        return requireImport(MemberWorkbookMode.UPDATE);
    }

    private List<String> createHeaders() {
        return List.of(DepartmentWorkbookColumns.NAME, DepartmentWorkbookColumns.PARENT,
                DepartmentWorkbookColumns.SORT_ORDER);
    }

    private List<String> updateHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add(DepartmentWorkbookColumns.DEPARTMENT_ID);
        headers.addAll(createHeaders());
        return headers;
    }

    private String tenantVersion(long tenantId) {
        IamTenantEntity tenant = tenants.findActive(tenantId);
        if (tenant == null || tenant.getVersion() == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return tenant.getVersion().toString();
    }

    private void expireDue(long tenantId) {
        for (IamDepartmentWorkbookEntity row : workbooks.expireDue(tenantId, utcNow())) {
            removeObject(row.getWorkbookPath());
            removeObject(row.getErrorReportPath());
        }
    }

    private String storeObject(long tenantId, long actorId, long taskId, String filename, byte[] content) {
        String objectName = OBJECT_PREFIX + "/" + tenantId + "/" + actorId + "/" + taskId + "/" + filename;
        String bucket = downloadProperties.getBucket();
        oss.uploadFile(bucket, objectName, new ByteArrayInputStream(content));
        return bucket + "/" + objectName;
    }

    private byte[] readObject(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }
        try {
            OssObjectInfo object = OssPathParser.parse(objectPath);
            return oss.getObjectBytes(object.bucket(), object.objectName());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void removeObject(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }
        try {
            OssObjectInfo object = OssPathParser.parse(objectPath);
            oss.removeFile(object.bucket(), object.objectName());
        } catch (RuntimeException ignored) {
        }
    }

    private static byte[] readUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.FILE_INVALID);
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(DepartmentWorkbookColumns.FILE_EXTENSION)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.FILE_INVALID);
        }
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.FILE_INVALID);
        }
    }

    private static MemberWorkbookFile fileOf(String filename, byte[] content) {
        return new MemberWorkbookFile(filename, DepartmentWorkbookColumns.CONTENT_TYPE,
                Base64.getEncoder().encodeToString(content));
    }

    private static DepartmentWorkbookOutcome outcomeOf(IamDepartmentWorkbookEntity row) {
        if (row.getResult() == null || row.getResult().isBlank()) {
            return null;
        }
        return IamJson.read(row.getResult(), OUTCOME_TYPE);
    }

    private static String rowReason(BizException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getCode() : exception.getMessage();
    }

    private static String failureReason(RuntimeException exception) {
        if (exception instanceof BizException biz) {
            String code = biz.getCode();
            if (IamReasonCode.ACTION_DENIED.getCode().equals(code)
                    || IamReasonCode.IDENTITY_INVALID.getCode().equals(code)) {
                return DepartmentWorkbookReasons.FAILURE_IDENTITY;
            }
        }
        return DepartmentWorkbookReasons.FAILURE_PROCESS;
    }

    private static String exportFailure(BizException exception) {
        String code = exception.getCode();
        if (IamReasonCode.ACTION_DENIED.getCode().equals(code) || IamReasonCode.IDENTITY_INVALID.getCode().equals(code)) {
            return DepartmentWorkbookReasons.EXPORT_DENIED;
        }
        return DepartmentWorkbookReasons.EXPORT_FAILED;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
