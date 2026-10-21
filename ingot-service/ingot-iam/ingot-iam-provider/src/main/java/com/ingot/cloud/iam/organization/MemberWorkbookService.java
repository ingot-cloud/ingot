package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.download.IamDownloadOwners;
import com.ingot.cloud.iam.download.IamDownloadSources;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.organization.MemberWorkbookExcel.MemberWorkbookOutcome;
import com.ingot.cloud.iam.organization.MemberWorkbookExcel.MemberWorkbookRowFailure;
import com.ingot.cloud.iam.organization.MemberWorkbookExcel.MemberWorkbookSheet;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberWorkbookRepository;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamMemberWorkbookEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberDepartmentInput;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberProfileInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.MemberWorkbookArtifact;
import com.ingot.framework.commons.model.iam.MemberWorkbookColumns;
import com.ingot.framework.commons.model.iam.MemberWorkbookExportInput;
import com.ingot.framework.commons.model.iam.MemberWorkbookFile;
import com.ingot.framework.commons.model.iam.MemberWorkbookHistoryItem;
import com.ingot.framework.commons.model.iam.MemberWorkbookKind;
import com.ingot.framework.commons.model.iam.MemberWorkbookLimits;
import com.ingot.framework.commons.model.iam.MemberWorkbookMode;
import com.ingot.framework.commons.model.iam.MemberWorkbookReasons;
import com.ingot.framework.commons.model.iam.MemberWorkbookTask;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.ResourceFieldContext;
import com.ingot.framework.commons.model.iam.TenantMemberCreateInput;
import com.ingot.framework.commons.model.iam.TenantMemberCreated;
import com.ingot.framework.download.DownloadCenter;
import com.ingot.framework.download.DownloadFile;
import com.ingot.framework.download.DownloadJobException;
import com.ingot.framework.download.DownloadRequest;
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
 * <p>登记成员表格导入任务并逐行复用入职、资料更新与任职替换；导出交给下载中心生成，不改动 JSON 导出。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberWorkbookService {
    private static final String TARGET = "member-workbook";
    private static final String XLSX_SUFFIX = MemberWorkbookColumns.FILE_EXTENSION;
    private static final Pattern PHONE_PATTERN = Pattern.compile(TenantMemberCreateInput.PHONE_PATTERN);
    private static final TypeReference<MemberWorkbookOutcome> OUTCOME_TYPE = new TypeReference<>() {
    };
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final MemberQueryService members;
    private final MemberCommandService commands;
    private final TenantMemberOnboarding onboarding;
    private final MemberWorkbookRepository workbooks;
    private final DepartmentQueryRepository departments;
    private final TenantRepository tenants;
    private final IamAuditWriter audits;
    private final DownloadCenter downloads;
    private final TransactionTemplate transaction;
    private final Executor executor;
    private final int snapshotPageSize;

    /**
     * 绑定身份、成员写入与表格任务存储。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成；执行器需排除调度器，
     * 并在提交时绑定当前认证，异步线程才能复用入职与任职写入。保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param members 成员查询
     * @param commands 任职与资料写入
     * @param onboarding 入职
     * @param workbooks 任务存储
     * @param departments 部门目录
     * @param tenants 组织版本
     * @param audits 同事务审计
     * @param downloads 下载中心
     * @param transactionManager 同一数据源事务
     * @param executors 应用异步执行器
     */
    @Autowired
    public MemberWorkbookService(IamAccess access, ResourceAccess scopes, MemberQueryService members,
            MemberCommandService commands, TenantMemberOnboarding onboarding, MemberWorkbookRepository workbooks,
            DepartmentQueryRepository departments, TenantRepository tenants, IamAuditWriter audits,
            DownloadCenter downloads, PlatformTransactionManager transactionManager,
            ObjectProvider<TaskExecutor> executors) {
        this(access, scopes, members, commands, onboarding, workbooks, departments, tenants, audits, downloads,
                transactionManager, applicationExecutor(executors), IamPages.MAX_SIZE);
    }

    /**
     * 指定快照分页大小，供测试覆盖超过单页上限的遍历。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param members 成员查询
     * @param commands 任职与资料写入
     * @param onboarding 入职
     * @param workbooks 任务存储
     * @param departments 部门目录
     * @param tenants 组织版本
     * @param audits 同事务审计
     * @param downloads 下载中心
     * @param transactionManager 同一数据源事务
     * @param executor 导入任务执行器
     * @param snapshotPageSize 快照遍历页大小
     */
    public MemberWorkbookService(IamAccess access, ResourceAccess scopes, MemberQueryService members,
            MemberCommandService commands, TenantMemberOnboarding onboarding, MemberWorkbookRepository workbooks,
            DepartmentQueryRepository departments, TenantRepository tenants, IamAuditWriter audits,
            DownloadCenter downloads, PlatformTransactionManager transactionManager, Executor executor,
            int snapshotPageSize) {
        this.access = access;
        this.scopes = scopes;
        this.members = members;
        this.commands = commands;
        this.onboarding = onboarding;
        this.workbooks = workbooks;
        this.departments = departments;
        this.tenants = tenants;
        this.audits = audits;
        this.downloads = downloads;
        this.transaction = new TransactionTemplate(transactionManager);
        this.executor = new DelegatingSecurityContextExecutor(executor == null ? Runnable::run : executor);
        this.snapshotPageSize = snapshotPageSize < 1 || snapshotPageSize > IamPages.MAX_SIZE
                ? IamPages.MAX_SIZE : snapshotPageSize;
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
     * 按当前创建字段可见性生成空白模板。
     *
     * @return 模板文件
     */
    public MemberWorkbookFile template() {
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
        List<String> headers = createHeaders();
        return fileOf(MemberWorkbookColumns.TEMPLATE_FILENAME, MemberWorkbookExcel.writeMembers(headers, List.of(), false));
    }

    /**
     * 校验组织版本后向下载中心登记更新导出，结果在「我的下载」查看。
     *
     * @param input 组织版本与可选成员范围
     * @return 下载任务 ID 与组织版本
     */
    public CreatedResource export(MemberWorkbookExportInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
        List<String> memberIds = input.memberIds() == null ? List.of() : List.copyOf(input.memberIds());
        DownloadRequest request = new DownloadRequest(IamDownloadOwners.of(actor), IamDownloadSources.MEMBER_WORKBOOK,
                IamDownloadSources.MEMBER_WORKBOOK_TITLE);
        CreatedResource created = transaction.execute(status -> {
            String version = tenantVersion(IamIds.require(actor.context().tenantId()));
            IamIds.requireVersion(input.expectedVersion(), version);
            long id = downloads.submit(request, () -> exportFile(actor, memberIds));
            audits.write(actor.context(), access.nextId(), TARGET, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, TARGET), Map.of("tenant", version));
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
        MemberWorkbookExcel.read(content);
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
        workbooks.expireDue(tenantId, utcNow());
        List<MemberWorkbookHistoryItem> items = new ArrayList<>();
        for (IamMemberWorkbookEntity row : workbooks.listRecentImports(tenantId, actorId)) {
            MemberWorkbookOutcome outcome = outcomeOf(row);
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
        IamMemberWorkbookEntity row = requireOwned(workbookId);
        MemberWorkbookOutcome outcome = outcomeOf(row);
        boolean passwords = row.getPasswordFile() != null && row.getPasswordFile().length > 0
                && !Boolean.TRUE.equals(row.getPasswordDownloaded());
        return new MemberWorkbookTask(IamIds.text(row.getId().longValueExact()), row.getKind(), row.getMode(),
                row.getStatus(), row.getTenantVersion(), row.getCreatedAt().toInstant(ZoneOffset.UTC),
                row.getExpiresAt().toInstant(ZoneOffset.UTC),
                row.getStatus() == ExportTaskStatus.FAILED ? row.getFailureReason() : null,
                outcome == null ? null : outcome.successCount(),
                outcome == null ? null : outcome.failCount(),
                row.getKind() == MemberWorkbookKind.IMPORT && row.getMode() == MemberWorkbookMode.CREATE
                        ? passwords : null);
    }

    /**
     * 下载任务产物；密码文件只能取一次。
     *
     * @param workbookId 任务 ID
     * @param artifact 产物
     * @return 文件
     */
    public MemberWorkbookFile file(String workbookId, MemberWorkbookArtifact artifact) {
        if (artifact == null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        IamMemberWorkbookEntity row = requireOwned(workbookId);
        if (row.getStatus() == ExportTaskStatus.PENDING || row.getStatus() == ExportTaskStatus.RUNNING) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        if (row.getStatus() != ExportTaskStatus.SUCCEEDED) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        if (artifact == MemberWorkbookArtifact.PASSWORDS) {
            byte[] passwords = workbooks.consumePasswords(row.getTenantId().longValueExact(),
                    row.getId().longValueExact());
            if (passwords == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return fileOf(MemberWorkbookColumns.PASSWORD_FILENAME, passwords);
        }
        byte[] content = artifact == MemberWorkbookArtifact.ERRORS ? row.getErrorReport() : row.getWorkbook();
        if (content == null || content.length == 0) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        String name = artifact == MemberWorkbookArtifact.ERRORS ? MemberWorkbookColumns.ERROR_FILENAME
                : row.getKind() == MemberWorkbookKind.EXPORT ? MemberWorkbookColumns.EXPORT_FILENAME
                        : MemberWorkbookColumns.TEMPLATE_FILENAME;
        return fileOf(name, content);
    }

    private CreatedResource register(ActiveIdentity actor, MemberWorkbookMode mode, byte[] workbook) {
        long tenantId = IamIds.require(actor.context().tenantId());
        CreatedResource created = transaction.execute(status -> {
            LocalDateTime now = utcNow();
            workbooks.expireDue(tenantId, now);
            String version = tenantVersion(tenantId);
            long id = access.nextId();
            IamMemberWorkbookEntity row = new IamMemberWorkbookEntity();
            row.setId(BigInteger.valueOf(id));
            row.setTenantId(BigInteger.valueOf(tenantId));
            row.setActorMemberId(BigInteger.valueOf(IamIds.require(actor.context().memberId())));
            row.setKind(MemberWorkbookKind.IMPORT);
            row.setMode(mode);
            row.setStatus(ExportTaskStatus.PENDING);
            row.setTenantVersion(version);
            row.setWorkbook(workbook);
            row.setPasswordDownloaded(Boolean.FALSE);
            row.setCreatedAt(now);
            row.setExpiresAt(now.plus(MemberWorkbookLimits.RETENTION));
            workbooks.insert(row);
            audits.write(actor.context(), access.nextId(), TARGET, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, TARGET), Map.of("tenant", version));
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
            IamMemberWorkbookEntity row = workbooks.findOwned(tenantId, IamIds.require(actor.context().memberId()),
                    workbookId);
            if (row == null) {
                workbooks.markFailed(tenantId, workbookId, MemberWorkbookReasons.FAILURE_PROCESS, utcNow());
                return;
            }
            if (row.getKind() != MemberWorkbookKind.IMPORT) {
                workbooks.markFailed(tenantId, workbookId, MemberWorkbookReasons.FAILURE_PROCESS, utcNow());
                return;
            }
            completeImport(actor, row);
        } catch (RuntimeException exception) {
            workbooks.markFailed(tenantId, workbookId, failureReason(exception), utcNow());
        }
    }

    private DownloadFile exportFile(ActiveIdentity actor, List<String> memberIds) {
        try {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
            List<ResourceDetail<MemberRecord>> details = collectExportMembers(actor, memberIds);
            MemberWorkbookPaths paths = catalog(actor);
            List<String> headers = updateHeaders();
            List<Map<String, String>> data = new ArrayList<>();
            for (ResourceDetail<MemberRecord> detail : details) {
                MemberRecord record = detail.record();
                if (record.status() == MemberStatus.REMOVED) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                values.put(MemberWorkbookColumns.MEMBER_ID, record.id());
                if (headers.contains(MemberWorkbookColumns.DISPLAY_NAME)) {
                    values.put(MemberWorkbookColumns.DISPLAY_NAME, text(record.displayName()));
                }
                if (headers.contains(MemberWorkbookColumns.PHONE)) {
                    values.put(MemberWorkbookColumns.PHONE, text(record.phone()));
                }
                if (headers.contains(MemberWorkbookColumns.EMAIL)) {
                    values.put(MemberWorkbookColumns.EMAIL, text(record.email()));
                }
                values.put(MemberWorkbookColumns.DEPARTMENTS, paths.format(record.departments()));
                data.add(values);
            }
            return new DownloadFile(MemberWorkbookColumns.EXPORT_FILENAME, MemberWorkbookColumns.CONTENT_TYPE,
                    MemberWorkbookExcel.writeMembers(headers, data, true));
        } catch (BizException exception) {
            throw new DownloadJobException(exportFailure(exception));
        }
    }

    private void completeImport(ActiveIdentity actor, IamMemberWorkbookEntity row) {
        MemberWorkbookSheet sheet = MemberWorkbookExcel.read(row.getWorkbook());
        MemberWorkbookPaths paths = catalog(actor);
        String ownerId = ownerMemberId(IamIds.require(actor.context().tenantId()));
        List<MemberWorkbookRowFailure> errors = new ArrayList<>();
        List<List<String>> passwords = new ArrayList<>();
        int success = 0;
        for (int index = 0; index < sheet.rows().size(); index++) {
            int excelRow = sheet.excelRow(index);
            try {
                TenantMemberCreated created = row.getMode() == MemberWorkbookMode.CREATE
                        ? importCreate(sheet.rows().get(index), sheet, paths)
                        : importUpdate(actor, sheet.rows().get(index), sheet, paths, ownerId);
                success++;
                if (created != null && Boolean.TRUE.equals(created.accountCreated())
                        && created.initialPassword() != null && !created.initialPassword().isBlank()) {
                    passwords.add(List.of(created.id(), text(sheet.rows().get(index).get(MemberWorkbookColumns.DISPLAY_NAME)),
                            text(sheet.rows().get(index).get(MemberWorkbookColumns.PHONE)), created.initialPassword()));
                }
            } catch (BizException exception) {
                errors.add(new MemberWorkbookRowFailure(excelRow, rowReason(exception)));
            }
        }
        byte[] errorReport = errors.isEmpty() ? null : MemberWorkbookExcel.writeErrors(errors);
        byte[] passwordFile = passwords.isEmpty() ? null : MemberWorkbookExcel.writePasswords(passwords);
        if (!workbooks.markSucceeded(IamIds.require(actor.context().tenantId()), row.getId().longValueExact(), null,
                IamJson.object(new MemberWorkbookOutcome(success, errors.size(), errors)), row.getWorkbook(),
                errorReport, passwordFile, utcNow())) {
            workbooks.markFailed(IamIds.require(actor.context().tenantId()), row.getId().longValueExact(),
                    MemberWorkbookReasons.FAILURE_PROCESS, utcNow());
        }
    }

    private TenantMemberCreated importCreate(Map<String, String> row, MemberWorkbookSheet sheet,
            MemberWorkbookPaths paths) {
        String name = cell(row, sheet, MemberWorkbookColumns.DISPLAY_NAME, true);
        String phone = cell(row, sheet, MemberWorkbookColumns.PHONE, true);
        if (name.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.NAME_REQUIRED);
        }
        if (phone.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.PHONE_REQUIRED);
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.PHONE_INVALID);
        }
        String email = sheet.has(MemberWorkbookColumns.EMAIL) ? cell(row, sheet, MemberWorkbookColumns.EMAIL, true) : null;
        List<MemberDepartmentBinding> departments = sheet.has(MemberWorkbookColumns.DEPARTMENTS)
                ? paths.parse(row.get(MemberWorkbookColumns.DEPARTMENTS)) : List.of();
        return onboarding.create(new TenantMemberCreateInput(name, phone, blankToNull(email), departments));
    }

    private TenantMemberCreated importUpdate(ActiveIdentity actor, Map<String, String> row, MemberWorkbookSheet sheet,
            MemberWorkbookPaths paths, String ownerId) {
        String memberId = text(row.get(MemberWorkbookColumns.MEMBER_ID));
        if (memberId.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.MEMBER_ID_REQUIRED);
        }
        ResourceDetail<MemberRecord> current = loadForWrite(actor, memberId);
        if (current.record().status() == MemberStatus.REMOVED) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.MEMBER_REMOVED);
        }
        if (sheet.has(MemberWorkbookColumns.DISPLAY_NAME) || sheet.has(MemberWorkbookColumns.PHONE)
                || sheet.has(MemberWorkbookColumns.EMAIL)) {
            String name = sheet.has(MemberWorkbookColumns.DISPLAY_NAME)
                    ? cell(row, sheet, MemberWorkbookColumns.DISPLAY_NAME, true) : null;
            String phone = sheet.has(MemberWorkbookColumns.PHONE)
                    ? cell(row, sheet, MemberWorkbookColumns.PHONE, true) : null;
            String email = sheet.has(MemberWorkbookColumns.EMAIL)
                    ? cell(row, sheet, MemberWorkbookColumns.EMAIL, false) : null;
            if (sheet.has(MemberWorkbookColumns.DISPLAY_NAME) && name.isBlank()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.NAME_REQUIRED);
            }
            if (sheet.has(MemberWorkbookColumns.PHONE) && phone.isBlank()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.PHONE_REQUIRED);
            }
            if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.PHONE_INVALID);
            }
            Set<String> supplied = new HashSet<>();
            supplied.add("expectedVersion");
            if (sheet.has(MemberWorkbookColumns.DISPLAY_NAME)) {
                supplied.add(MemberFieldKey.VALUE_DISPLAY_NAME);
            }
            if (sheet.has(MemberWorkbookColumns.PHONE)) {
                supplied.add(MemberFieldKey.VALUE_PHONE);
            }
            if (sheet.has(MemberWorkbookColumns.EMAIL)) {
                supplied.add(MemberFieldKey.VALUE_EMAIL);
            }
            members.patch(AuthorizationDomain.TENANT, memberId,
                    new MemberProfileInput(current.version(), name, null, phone, email, supplied));
            current = loadForWrite(actor, memberId);
        }
        if (sheet.has(MemberWorkbookColumns.DEPARTMENTS)) {
            List<MemberDepartmentBinding> next = paths.parse(row.get(MemberWorkbookColumns.DEPARTMENTS));
            if (Objects.equals(ownerId, memberId) && departmentsChanged(current.record().departments(), next)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(),
                        MemberWorkbookReasons.OWNER_DEPARTMENTS);
            }
            commands.replaceDepartments(memberId, new MemberDepartmentInput(current.version(), next));
        }
        return null;
    }

    private ResourceDetail<MemberRecord> loadForWrite(ActiveIdentity actor, String memberId) {
        IamAction action = access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_UPDATE, false)
                ? IamAction.TENANT_MEMBER_UPDATE : IamAction.TENANT_MEMBER_DEPARTMENTS;
        PageResponse<ResourceDetail<MemberRecord>> page = members.listProjectedByIds(actor, action, List.of(memberId));
        if (page.items().isEmpty()) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND.getCode(), MemberWorkbookReasons.MEMBER_NOT_FOUND);
        }
        return page.items().getFirst();
    }

    private List<ResourceDetail<MemberRecord>> collectExportMembers(ActiveIdentity actor, List<String> requested) {
        if (requested != null && !requested.isEmpty()) {
            return members.listProjectedByIds(actor, IamAction.TENANT_MEMBER_EXPORT, requested).items();
        }
        List<ResourceDetail<MemberRecord>> ids = new ArrayList<>();
        int page = 1;
        long total = -1;
        while (true) {
            PageResponse<ResourceDetail<MemberRecord>> result = members.listProjected(actor,
                    IamAction.TENANT_MEMBER_EXPORT, page, snapshotPageSize, null, null);
            if (total < 0) {
                total = result.total();
            }
            ids.addAll(result.items());
            if (result.items().isEmpty() || ids.size() >= total || result.items().size() < snapshotPageSize) {
                break;
            }
            page++;
        }
        return ids;
    }

    private MemberWorkbookPaths catalog(ActiveIdentity actor) {
        long tenantId = IamIds.require(actor.context().tenantId());
        ObjectScope scope;
        try {
            scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
        } catch (BizException exception) {
            if (!IamReasonCode.ACTION_DENIED.getCode().equals(exception.getCode())) {
                throw exception;
            }
            scope = ObjectScope.all();
        }
        return new MemberWorkbookPaths(departments.list(tenantId, scope));
    }

    private IamMemberWorkbookEntity requireOwned(String workbookId) {
        ActiveIdentity actor = access.requireCurrent();
        long tenantId = IamIds.require(actor.context().tenantId());
        long actorId = IamIds.require(actor.context().memberId());
        workbooks.expireDue(tenantId, utcNow());
        IamMemberWorkbookEntity row = workbooks.findOwned(tenantId, actorId, IamIds.require(workbookId));
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        requireTaskAction(row);
        return row;
    }

    private ActiveIdentity requireImport(MemberWorkbookMode mode) {
        if (mode == MemberWorkbookMode.CREATE) {
            return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
        }
        if (access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_UPDATE, false)) {
            return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_UPDATE);
        }
        return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_DEPARTMENTS);
    }

    private ActiveIdentity requireHistory() {
        if (access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE, false)) {
            return access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE);
        }
        return requireImport(MemberWorkbookMode.UPDATE);
    }

    private void requireTaskAction(IamMemberWorkbookEntity row) {
        if (row.getKind() == MemberWorkbookKind.EXPORT) {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
            return;
        }
        requireImport(row.getMode());
    }

    private List<String> createHeaders() {
        List<String> headers = new ArrayList<>();
        Map<String, FieldVisibility> visibility = fieldVisibility();
        addIfVisible(headers, MemberWorkbookColumns.DISPLAY_NAME, MemberFieldKey.VALUE_DISPLAY_NAME, visibility);
        addIfVisible(headers, MemberWorkbookColumns.PHONE, MemberFieldKey.VALUE_PHONE, visibility);
        addIfVisible(headers, MemberWorkbookColumns.EMAIL, MemberFieldKey.VALUE_EMAIL, visibility);
        headers.add(MemberWorkbookColumns.DEPARTMENTS);
        return headers;
    }

    private List<String> updateHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add(MemberWorkbookColumns.MEMBER_ID);
        headers.addAll(createHeaders());
        return headers;
    }

    private Map<String, FieldVisibility> fieldVisibility() {
        try {
            ResourceFieldContext context = members.tenantContext();
            return context.fieldVisibility();
        } catch (BizException exception) {
            return Map.of();
        }
    }

    private static void addIfVisible(List<String> headers, String column, String field,
            Map<String, FieldVisibility> visibility) {
        FieldVisibility current = visibility.get(field);
        if (current == FieldVisibility.HIDDEN) {
            return;
        }
        headers.add(column);
    }

    private String tenantVersion(long tenantId) {
        IamTenantEntity tenant = tenants.findActive(tenantId);
        if (tenant == null || tenant.getVersion() == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return tenant.getVersion().toString();
    }

    private String ownerMemberId(long tenantId) {
        IamTenantEntity tenant = tenants.findActive(tenantId);
        if (tenant == null || tenant.getOwnerMemberId() == null) {
            return "";
        }
        return IamIds.text(tenant.getOwnerMemberId().longValueExact());
    }

    private static byte[] readUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.FILE_INVALID);
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(XLSX_SUFFIX)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.FILE_INVALID);
        }
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.FILE_INVALID);
        }
    }

    private static MemberWorkbookFile fileOf(String filename, byte[] content) {
        return new MemberWorkbookFile(filename, MemberWorkbookColumns.CONTENT_TYPE,
                Base64.getEncoder().encodeToString(content));
    }

    private static MemberWorkbookOutcome outcomeOf(IamMemberWorkbookEntity row) {
        if (row.getResult() == null || row.getResult().isBlank()) {
            return null;
        }
        return IamJson.read(row.getResult(), OUTCOME_TYPE);
    }

    private static String cell(Map<String, String> row, MemberWorkbookSheet sheet, String header, boolean rejectMask) {
        String value = text(row.get(header));
        if (rejectMask && sheet.has(header) && value.contains(MemberWorkbookColumns.MASK_MARK)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.MASKED_VALUE);
        }
        return value;
    }

    private static boolean departmentsChanged(List<MemberDepartmentView> current, List<MemberDepartmentBinding> next) {
        List<String> left = new ArrayList<>();
        if (current != null) {
            List<MemberDepartmentView> ordered = new ArrayList<>(current);
            ordered.sort((first, second) -> Boolean.compare(second.primary(), first.primary()));
            for (MemberDepartmentView item : ordered) {
                left.add(item.id() + ":" + item.primary());
            }
        }
        List<String> right = new ArrayList<>();
        for (MemberDepartmentBinding item : next) {
            right.add(item.id() + ":" + item.primary());
        }
        return !left.equals(right);
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
                return MemberWorkbookReasons.FAILURE_IDENTITY;
            }
        }
        return MemberWorkbookReasons.FAILURE_PROCESS;
    }

    private static String exportFailure(BizException exception) {
        String code = exception.getCode();
        if (IamReasonCode.ACTION_DENIED.getCode().equals(code) || IamReasonCode.IDENTITY_INVALID.getCode().equals(code)) {
            return MemberWorkbookReasons.EXPORT_DENIED;
        }
        return MemberWorkbookReasons.EXPORT_FAILED;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
