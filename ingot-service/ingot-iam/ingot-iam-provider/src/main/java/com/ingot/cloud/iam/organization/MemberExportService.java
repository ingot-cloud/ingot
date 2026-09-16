package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.MemberExportRepository;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamMemberExportEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.ExportTask;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>异步登记成员导出任务到共享库，快照遍历全部授权记录；下载时再次校验操作、范围和字段策略。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberExportService {
    private static final String TARGET = "member-export";
    private static final String FAILURE_SNAPSHOT = "SNAPSHOT_FAILED";
    private static final String FAILURE_IDENTITY = "IDENTITY_INVALID";
    private static final Duration RETENTION = Duration.ofHours(24);
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final MemberQueryService members;
    private final MemberExportRepository exports;
    private final IamAuditWriter audits;
    private final TenantRepository tenants;
    private final TransactionTemplate transaction;
    private final Executor executor;
    private final int snapshotPageSize;

    /**
     * 绑定身份、范围与共享任务存储。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成；执行器需排除调度器，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param members 成员查询
     * @param exports 导出任务存储
     * @param audits 同事务审计
     * @param tenants 组织版本
     * @param transactionManager 同一数据源事务
     * @param executors 应用异步执行器；排除调度器，测试可空并同步执行
     */
    @Autowired
    public MemberExportService(IamAccess access, ResourceAccess scopes, MemberQueryService members,
                               MemberExportRepository exports, IamAuditWriter audits, TenantRepository tenants,
                               PlatformTransactionManager transactionManager,
                               ObjectProvider<TaskExecutor> executors) {
        this(access, scopes, members, exports, audits, tenants, transactionManager,
                applicationExecutor(executors), IamPages.MAX_SIZE);
    }

    /**
     * 指定快照分页大小，供测试覆盖超过单页上限的遍历。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param members 成员查询
     * @param exports 导出任务存储
     * @param audits 同事务审计
     * @param tenants 组织版本
     * @param transactionManager 同一数据源事务
     * @param executor 任务执行器
     * @param snapshotPageSize 快照遍历页大小
     */
    public MemberExportService(IamAccess access, ResourceAccess scopes, MemberQueryService members,
                               MemberExportRepository exports, IamAuditWriter audits, TenantRepository tenants,
                               PlatformTransactionManager transactionManager, Executor executor,
                               int snapshotPageSize) {
        this.access = access;
        this.scopes = scopes;
        this.members = members;
        this.exports = exports;
        this.audits = audits;
        this.tenants = tenants;
        this.transaction = new TransactionTemplate(transactionManager);
        this.executor = executor == null ? Runnable::run : executor;
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
     * 校验组织版本后登记导出任务，提交后再异步快照，不把联系方式原值写入审计。
     *
     * @param input 组织版本
     * @return 导出任务 ID
     */
    public CreatedResource create(VersionInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
        long tenantId = IamIds.require(actor.context().tenantId());
        CreatedResource created = transaction.execute(status -> {
            LocalDateTime now = utcNow();
            exports.expireDue(tenantId, now);
            String version = tenantVersion(tenantId);
            IamIds.requireVersion(input.expectedVersion(), version);
            long exportId = access.nextId();
            IamMemberExportEntity row = new IamMemberExportEntity();
            row.setId(BigInteger.valueOf(exportId));
            row.setTenantId(BigInteger.valueOf(tenantId));
            row.setActorMemberId(BigInteger.valueOf(IamIds.require(actor.context().memberId())));
            row.setTenantVersion(version);
            row.setStatus(ExportTaskStatus.PENDING);
            row.setCreatedAt(now);
            row.setExpiresAt(now.plus(RETENTION));
            exports.insert(row);
            audits.write(actor.context(), exportId, TARGET, IamIds.text(tenantId), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, TARGET), Map.of("tenant", version));
            return new CreatedResource(IamIds.text(exportId), version);
        });
        if (created == null) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        executor.execute(() -> snapshot(actor, IamIds.require(created.id())));
        return created;
    }

    /**
     * 再次校验导出操作、范围与字段策略后返回快照交集，过期或失败任务不能当成功下载。
     *
     * @param exportId 任务 ID
     * @return 投影后的完整成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> download(String exportId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
        long tenantId = IamIds.require(actor.context().tenantId());
        long id = IamIds.require(exportId);
        LocalDateTime now = utcNow();
        exports.expireDue(tenantId, now);
        IamMemberExportEntity task = exports.find(tenantId, id);
        if (task == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        if (task.getStatus() == ExportTaskStatus.PENDING || task.getStatus() == ExportTaskStatus.RUNNING) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        if (task.getStatus() != ExportTaskStatus.SUCCEEDED) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        ObjectScope scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_EXPORT);
        if (scope.coversNone()) {
            return IamPages.complete(List.of());
        }
        return members.listProjectedByIds(actor, IamAction.TENANT_MEMBER_EXPORT,
                MemberExportRepository.memberIds(task));
    }

    /**
     * 返回任务生命周期状态，不交付成员快照；进行中也可读取，过期后标为 {@code EXPIRED}。
     *
     * @param exportId 任务 ID
     * @return 任务状态
     */
    public ExportTask status(String exportId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
        long tenantId = IamIds.require(actor.context().tenantId());
        long id = IamIds.require(exportId);
        LocalDateTime now = utcNow();
        exports.expireDue(tenantId, now);
        IamMemberExportEntity task = exports.find(tenantId, id);
        if (task == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        String failure = task.getStatus() == ExportTaskStatus.FAILED ? task.getFailureReason() : null;
        return new ExportTask(IamIds.text(task.getId().longValueExact()), task.getStatus(), task.getTenantVersion(),
                task.getExpiresAt().toInstant(ZoneOffset.UTC), failure);
    }

    private void snapshot(ActiveIdentity actor, long exportId) {
        long tenantId = IamIds.require(actor.context().tenantId());
        if (!exports.markRunning(tenantId, exportId)) {
            return;
        }
        try {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_EXPORT);
            List<String> ids = collectMemberIds(actor);
            if (!exports.markSucceeded(tenantId, exportId, ids, utcNow())) {
                exports.markFailed(tenantId, exportId, FAILURE_SNAPSHOT, utcNow());
            }
        } catch (RuntimeException exception) {
            exports.markFailed(tenantId, exportId, failureReason(exception), utcNow());
        }
    }

    private List<String> collectMemberIds(ActiveIdentity actor) {
        List<String> ids = new ArrayList<>();
        int page = 1;
        long total = -1;
        while (true) {
            PageResponse<ResourceDetail<MemberRecord>> result = members.listProjected(actor,
                    IamAction.TENANT_MEMBER_EXPORT, page, snapshotPageSize, null, null);
            if (total < 0) {
                total = result.total();
            }
            for (ResourceDetail<MemberRecord> item : result.items()) {
                ids.add(item.record().id());
            }
            if (result.items().isEmpty() || ids.size() >= total || result.items().size() < snapshotPageSize) {
                break;
            }
            page++;
        }
        return ids;
    }

    private String tenantVersion(long tenantId) {
        IamTenantEntity tenant = tenants.findActive(tenantId);
        if (tenant == null || tenant.getVersion() == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return tenant.getVersion().toString();
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String failureReason(RuntimeException exception) {
        if (exception instanceof BizException biz) {
            String code = biz.getCode();
            if (IamReasonCode.ACTION_DENIED.getCode().equals(code)
                    || IamReasonCode.IDENTITY_INVALID.getCode().equals(code)) {
                return FAILURE_IDENTITY;
            }
        }
        return FAILURE_SNAPSHOT;
    }
}
