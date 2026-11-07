package com.ingot.cloud.iam.application.roleapplication;

import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationReadyEvent;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationWork;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationItemEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationTaskEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;

import com.ingot.cloud.iam.application.assignment.model.AssignmentRecord;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeItem;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradePreviewItem;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationChangeInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationConfirmInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationItem;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationPrepareInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationRefreshInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationTask;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationPublishInput;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.cloud.iam.domain.role.RoleRevisionDistribution;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationItemStatus;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationSelection;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationStatus;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.infrastructure.persistence.RoleVersionApplicationRepository;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>编排角色版本应用的持久集合、全量预检和带租约的事务批次。</p>
 * <p>业务域门禁由内部适配器提供；工作线程不使用HTTP上下文或登录令牌。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@Slf4j
public class RoleVersionApplicationService implements RoleVersionApplicationWork {
    private static final List<RoleVersionApplicationItemStatus> UNFINISHED = List.of(RoleVersionApplicationItemStatus.PENDING,
            RoleVersionApplicationItemStatus.BLOCKED);
    private static final TypeReference<AssignmentRecord> ORIGINAL = new TypeReference<>() { };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() { };
    private static final TypeReference<AssignmentUpgradePreviewItem> PREVIEW = new TypeReference<>() { };
    private static final int MAX_REASON_LENGTH = 512;
    private final IamAccess access;
    private final RoleVersionApplicationPort domain;
    private final RoleVersionApplicationRepository store;
    private final TransactionTemplate transaction;
    private final AuthorizationAuditWriter audits;
    private final ApplicationEventPublisher events;
    private static final String APPLICATION_AUDIT = "ROLE_VERSION_APPLICATION";
    private final String worker = UUID.randomUUID().toString();

    /**
     * 构造事务模板，所有依赖必需且通过构造器注入。
     * @param access ID生成，不用于业务域准入
     * @param domain 业务域访问与门禁
     * @param store 任务存储
     * @param audits 审计写入
     * @param manager 事务管理器
     * @param events 提交后的领取提示
     */
    public RoleVersionApplicationService(IamAccess access, RoleVersionApplicationPort domain, RoleVersionApplicationRepository store,
            PlatformTransactionManager manager, AuthorizationAuditWriter audits, ApplicationEventPublisher events) {
        this.access = access; this.domain = domain; this.store = store;
        this.transaction = new TransactionTemplate(manager); this.audits = audits; this.events = events;
    }

    /**
     * 为既有版本准备应用，准备幂等重放不会重新取集合。
     * @param roleId 角色ID
     * @param input 目标和集合
     * @return 已持久化计划
     */
    public RoleVersionApplicationTask prepare(String roleId, RoleVersionApplicationPrepareInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission);
            var existing = store.findPrepared(IamIds.require(roleId), input.idempotencyKey());
            if (existing != null) return replay(existing, admission, IamJsonCodec.object(input));
            return prepareLocked(roleId, input, governed(), IamJsonCodec.object(input));
        });
    }

    /**
     * 发布和计划原子创建；预检冲突保留新版本，分配不变。
     * @param roleId 角色ID
     * @param input 发布及集合
     * @return 发布版本ID在计划targetRevisionId中
     */
    public RoleVersionApplicationTask publishAndPrepare(String roleId, RoleVersionApplicationPublishInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission);
            var existing = store.findPrepared(IamIds.require(roleId), input.idempotencyKey());
            if (existing != null) return replay(existing, admission, IamJsonCodec.object(input));
            var published = domain.publish(roleId, input.publish());
            var prepare = new RoleVersionApplicationPrepareInput(published.id(), input.selection(), input.sourceRevisionIds(),
                    input.assignmentIds(), input.idempotencyKey());
            return prepareLocked(roleId, prepare, governed(), IamJsonCodec.object(input));
        });
    }

    /** 读取当前可治理集合的任务状态和计数。 */
    public RoleVersionApplicationTask get(String roleId, String id) {
        var admission = governed();
        return view(requireTask(roleId, id, admission), readScope(admission));
    }

    /** 分页读取角色任务，不披露越权分配计数。 */
    public PageResponse<RoleVersionApplicationTask> list(String roleId, int page, int pageSize) {
        IamPages.require(page, pageSize);
        var admission = governed(); requireRole(roleId, admission);
        var rows = store.pageTasks(IamIds.require(roleId), page, pageSize);
        var scope = readScope(admission);
        var records = rows.getRecords();
        if (records.isEmpty()) return IamPages.of(List.of(), rows.getTotal(), page, pageSize);
        Map<BigInteger, IamRoleRevisionEntity> targets = new LinkedHashMap<>();
        domain.findRevisions(records.stream().map(IamRoleVersionApplicationTaskEntity::getTargetRevisionId).distinct().toList())
                .forEach(row -> targets.put(row.getId(), row));
        var counts = store.counts(records.stream().map(IamRoleVersionApplicationTaskEntity::getId).toList(), scope);
        return IamPages.of(records.stream().map(row -> view(row, targets.get(row.getTargetRevisionId()),
                counts.get(row.getId()))).toList(), rows.getTotal(), page, pageSize);
    }

    /**
     * 分页选择角色关联的分配，不展开组或披露范围外计数。
     * @param roleId 角色
     * @param page 页码
     * @param size 页大小
     * @return 同边界的候选及分配版本
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> candidates(String roleId, int page, int size) {
        IamPages.require(page, size); var admission = governed(); requireRole(roleId, admission);
        var rows = domain.pageAssignments(IamIds.require(roleId), page, size, readScope(admission));
        return IamPages.details(rows.getRecords().stream().map(row -> ResourceDetailFactory.of(
                domain.snapshot(admission, row), row.getVersion().toString())).toList(),
                rows.getTotal(), page, size);
    }

    /**
     * 角色有效分配的版本分布；展示与应用候选边界相同。
     * @param roleId 角色
     * @return 可治理分配数
     */
    public List<RoleRevisionDistribution> distribution(String roleId) {
        var admission = governed(); requireRole(roleId, admission);
        return domain.distribution(IamIds.require(roleId), readScope(admission));
    }

    /** 分页读取同一可见性边界下的计划明细及完整权限差异。 */
    public PageResponse<RoleVersionApplicationItem> items(String roleId, String id, int page, int pageSize) {
        IamPages.require(page, pageSize);
        var admission = governed(); var task = requireTask(roleId, id, admission);
        var rows = store.pageItems(task.getId().longValueExact(), page, pageSize, readScope(admission));
        return IamPages.of(rows.getRecords().stream().map(this::item).toList(), rows.getTotal(), page, pageSize);
    }

    /**
     * 显式调整选定集合的部分参数或排除状态，并重新预检整个未完成集合。
     * @param roleId 角色ID
     * @param id 任务ID
     * @param input 计划版本和明确选择的覆盖内容
     * @return 最新待确认计划
     */
    public RoleVersionApplicationTask configure(String roleId, String id, RoleVersionApplicationChangeInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission); var task = lockedTask(roleId, id, governed());
            editable(task, input.expectedVersion());
            if (input.all() == (input.itemIds() != null && !input.itemIds().isEmpty()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            if (input.scopeBindings() == null && input.excluded() == null)
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            var chosen = input.itemIds() == null ? List.<String>of() : List.copyOf(input.itemIds());
            List<String> changed = new ArrayList<>();
            scan(task, scope(admission), null, row -> {
                if (!input.all() && !chosen.contains(row.getId().toString())) return;
                if (row.getStatus() != RoleVersionApplicationItemStatus.PENDING
                        && row.getStatus() != RoleVersionApplicationItemStatus.BLOCKED
                        && row.getStatus() != RoleVersionApplicationItemStatus.EXCLUDED) return;
                changed.add(row.getId().toString());
                if (input.scopeBindings() != null) {
                    var merged = new LinkedHashMap<>(bindings(row)); merged.putAll(input.scopeBindings());
                    row.setConfiguredBindings(IamJsonCodec.object(merged));
                }
                if (input.excluded() != null)
                    row.setStatus(input.excluded() ? RoleVersionApplicationItemStatus.EXCLUDED : RoleVersionApplicationItemStatus.PENDING);
                store.save(row);
            });
            if (!input.all() && (chosen.stream().distinct().count() != chosen.size() || !changed.containsAll(chosen)))
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            preflight(task, governed(), false); bump(task); store.save(task);
            return view(task, scope(admission));
        });
    }

    /** 未完成分配按最新事实重新预检；不会纳入快照后新增的分配。 */
    public RoleVersionApplicationTask refresh(String roleId, String id, RoleVersionApplicationRefreshInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission); var task = lockedTask(roleId, id, governed());
            editable(task, input.expectedVersion());
            preflight(task, governed(), true); bump(task); store.save(task);
            return view(task, scope(admission));
        });
    }

    /**
     * 确认最新无冲突计划；全量fresh预检通过后才进入异步领取状态。
     * @param roleId 角色ID
     * @param id 计划ID
     * @param input 乐观锁及确认幂等键
     * @return 异步任务状态
     */
    public RoleVersionApplicationTask confirm(String roleId, String id, RoleVersionApplicationConfirmInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission); var task = lockedTask(roleId, id, governed());
            if (input.idempotencyKey().equals(task.getConfirmKey())) return view(task, scope(admission));
            editable(task, input.expectedVersion());
            if (task.getStatus() != RoleVersionApplicationStatus.READY) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            preflight(task, governed(), false);
            if (task.getStatus() == RoleVersionApplicationStatus.READY) {
                var running = store.runningForRole(task.getRoleId());
                if (running != null && !running.getId().equals(task.getId())) throw new BizException(IamReasonCode.REVISION_CONFLICT);
                task.setStatus(RoleVersionApplicationStatus.RUNNING);
                task.setConfirmKey(input.idempotencyKey());
                task.setActorAccountId(new BigInteger(admission.actor().context().accountId()));
                task.setActorMemberId(new BigInteger(admission.actor().context().memberId()));
                task.setLeaseOwner(null); task.setLeaseUntil(null);
                var ready = new RoleVersionApplicationReadyEvent(task.getId(), task.getRoleId());
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            events.publishEvent(ready);
                        } catch (RuntimeException failure) {
                            // 提示丢失由持久任务扫描补齐，不影响已提交的确认结果。
                            log.warn("角色版本应用唤醒失败 task={}", ready.taskId(), failure);
                        }
                    }
                });
            }
            bump(task); store.save(task);
            return view(task, scope(admission));
        });
    }

    /**
     * 显式执行一页候选的单批事务；生产调度使用按ID入口，不调用定频扫描。
     */
    public void executeAvailable() {
        for (var candidate : store.runnable(worker)) {
            executeTask(candidate.getId());
        }
    }

    /** 游标发现可领取任务，不读取快照JSON。 */
    @Override
    public List<BigInteger> discover(BigInteger after, int limit) {
        return store.runnable(worker, after, limit).stream().map(IamRoleVersionApplicationTaskEntity::getId).toList();
    }

    /** 按持久任务ID领取并执行一批；继续结果不替代数据库租约校验。 */
    @Override
    public BatchResult executeTask(BigInteger taskId) {
        var candidate = store.runnableTask(taskId, worker);
        if (candidate == null) return BatchResult.STOP;
        try {
            Boolean claimed = transaction.execute(status -> {
                lockWorker(candidate);
                return store.claim(candidate.getId(), worker);
            });
            if (!Boolean.TRUE.equals(claimed)) return BatchResult.CONFLICT;
            return Boolean.TRUE.equals(executeBatch(candidate)) ? BatchResult.MORE : BatchResult.STOP;
        } catch (RuntimeException exception) {
            log.warn("角色版本应用批次暂停 task={}", candidate.getId(), exception);
            if (!pause(candidate, exception)) throw exception;
            return BatchResult.STOP;
        }
    }

    private Boolean executeBatch(IamRoleVersionApplicationTaskEntity candidate) {
        return transaction.execute(status -> {
            lockWorker(candidate); var task = store.lock(candidate.getId().longValueExact());
            if (task.getStatus() != RoleVersionApplicationStatus.RUNNING || !worker.equals(task.getLeaseOwner())) return false;
            if (!store.claim(task.getId(), worker)) return false;
            task = store.find(task.getId().longValueExact());
            var admission = domain.restore(task); requireRole(task.getRoleId().toString(), admission, true);
            var batch = store.scan(task.getId().longValueExact(), BigInteger.ZERO, ObjectScope.all(), UNFINISHED);
            if (!batch.isEmpty()) {
                var input = command(task, batch);
                domain.apply(admission, input);
                for (var row : batch) {
                    row.setStatus(RoleVersionApplicationItemStatus.SUCCEEDED); row.setFailureReason(null); store.save(row);
                }
            }
            if (store.scan(task.getId().longValueExact(), BigInteger.ZERO, ObjectScope.all(), UNFINISHED).isEmpty()) {
                task.setStatus(RoleVersionApplicationStatus.SUCCEEDED); task.setLeaseOwner(null); task.setLeaseUntil(null);
            }
            bump(task); store.save(task);
            return task.getStatus() == RoleVersionApplicationStatus.RUNNING;
        });
    }

    private boolean pause(IamRoleVersionApplicationTaskEntity candidate, RuntimeException failure) {
        try {
            transaction.executeWithoutResult(status -> {
                lockWorker(candidate); var task = store.lock(candidate.getId().longValueExact());
                if (task.getStatus() != RoleVersionApplicationStatus.RUNNING || !worker.equals(task.getLeaseOwner())) return;
                task.setStatus(RoleVersionApplicationStatus.PAUSED); task.setFailureReason(reason(failure));
                var batch = store.scan(task.getId().longValueExact(), BigInteger.ZERO, ObjectScope.all(), UNFINISHED);
                for (var row : batch) {
                    row.setStatus(RoleVersionApplicationItemStatus.BLOCKED); row.setFailureReason(reason(failure)); row.setPreviewSnapshot(null); store.save(row);
                }
                task.setLeaseOwner(null); task.setLeaseUntil(null); bump(task); store.save(task);
            });
            return true;
        } catch (RuntimeException unavailable) {
            // 数据库不可用时保持持久租约；到期后恢复会重新检查身份和授权，不作fail-open。
            log.warn("无法持久化任务暂停 task={}", candidate.getId(), unavailable);
            return false;
        }
    }

    private RoleVersionApplicationTask prepareLocked(String roleId, RoleVersionApplicationPrepareInput input,
            AuthorizationAdmission admission, String signature) {
        var target = domain.requireTarget(roleId, input.targetRevisionId());
        var sources = input.sourceRevisionIds() == null ? List.<BigInteger>of()
                : input.sourceRevisionIds().stream().map(v -> BigInteger.valueOf(IamIds.require(v))).distinct().toList();
        var selected = input.assignmentIds() == null ? List.<BigInteger>of()
                : input.assignmentIds().stream().map(v -> BigInteger.valueOf(IamIds.require(v))).distinct().toList();
        if (input.selection() == RoleVersionApplicationSelection.SOURCE_REVISIONS && sources.isEmpty()
                || input.selection() == RoleVersionApplicationSelection.ASSIGNMENTS && selected.isEmpty())
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        for (var source : sources) {
            var row = domain.findRevision(source.longValueExact());
            if (row == null || !row.getRoleId().equals(target.getRoleId())) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var task = new IamRoleVersionApplicationTaskEntity();
        task.setId(BigInteger.valueOf(access.nextId())); task.setRoleId(target.getRoleId());
        task.setTargetRevisionId(target.getId()); task.setCreatedBy(new BigInteger(admission.actor().context().memberId()));
        task.setActorAccountId(new BigInteger(admission.actor().context().accountId()));
        task.setActorMemberId(task.getCreatedBy()); task.setSelection(input.selection());
        task.setSelectionSnapshot(signature); task.setPrepareKey(input.idempotencyKey());
        task.setStatus(RoleVersionApplicationStatus.PREPARING); task.setVersion(BigInteger.ZERO);
        task.setCreatedAt(now()); task.setUpdatedAt(task.getCreatedAt()); store.insert(task);
        BigInteger cursor = BigInteger.ZERO; long selectedCount = 0;
        var scope = scope(admission);
        requireCollectionCoverage(roleId, input, sources, scope);
        while (true) {
            var rows = domain.scanAssignments(IamIds.require(roleId), cursor,
                    input.selection() == RoleVersionApplicationSelection.SOURCE_REVISIONS ? sources : null,
                    input.selection() == RoleVersionApplicationSelection.ASSIGNMENTS ? selected : null, scope,
                    RoleVersionApplicationRepository.BATCH_SIZE);
            if (rows.isEmpty()) break;
            for (var assignment : rows) {
                var original = domain.snapshot(admission, assignment);
                var row = new IamRoleVersionApplicationItemEntity(); row.setId(BigInteger.valueOf(access.nextId()));
                row.setTaskId(task.getId()); row.setAssignmentId(assignment.getId());
                row.setOriginalSnapshot(IamJsonCodec.object(original)); row.setExpectedAssignmentVersion(assignment.getVersion());
                row.setConfiguredBindings(IamJsonCodec.object(original.assignment().scopeBindings()));
                row.setStatus(assignment.getStatus() != GrantStatus.ACTIVE ? RoleVersionApplicationItemStatus.REVOKED
                        : original.assignment().validUntil() != null && !Instant.now().isBefore(original.assignment().validUntil())
                            ? RoleVersionApplicationItemStatus.EXPIRED
                        : assignment.getRevisionId().equals(target.getId()) ? RoleVersionApplicationItemStatus.ALREADY_TARGET
                        : RoleVersionApplicationItemStatus.PENDING);
                store.insert(row); selectedCount++;
            }
            cursor = rows.getLast().getId();
        }
        if (input.selection() == RoleVersionApplicationSelection.ASSIGNMENTS && selectedCount != selected.size())
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        preflight(task, admission, false, true); store.save(task);
        audits.write(admission.actor().context(), access.nextId(), APPLICATION_AUDIT, task.getId().toString(),
                AuditChangeType.CREATE, Map.of(), Map.of(AuditField.ROLE_REVISION, task.getTargetRevisionId().toString(),
                        AuditField.RECIPIENT_SELECTION, task.getSelection().getValue()), Map.of(APPLICATION_AUDIT, task.getVersion().toString()));
        return view(task, scope);
    }

    private void requireCollectionCoverage(String roleId, RoleVersionApplicationPrepareInput input,
            List<BigInteger> sources, ObjectScope scope) {
        if (scope.coversAll() || input.selection() == RoleVersionApplicationSelection.ASSIGNMENTS) return;
        BigInteger cursor = BigInteger.ZERO;
        while (true) {
            var rows = domain.scanAssignments(IamIds.require(roleId), cursor,
                    input.selection() == RoleVersionApplicationSelection.SOURCE_REVISIONS ? sources : null,
                    null, ObjectScope.all(), RoleVersionApplicationRepository.BATCH_SIZE);
            if (rows.isEmpty()) return;
            for (var row : rows) {
                if (row.getStatus() == GrantStatus.ACTIVE
                        && (row.getValidUntil() == null || row.getValidUntil().isAfter(now()))
                        && !com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql.matches(scope, row.getId().longValueExact()))
                    throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            cursor = rows.getLast().getId();
        }
    }

    private void preflight(IamRoleVersionApplicationTaskEntity task, AuthorizationAdmission admission, boolean refresh) {
        preflight(task, admission, refresh, false);
    }

    private void preflight(IamRoleVersionApplicationTaskEntity task, AuthorizationAdmission admission, boolean refresh, boolean reuse) {
        var visible = scope(admission);
        boolean blocked = false;
        BigInteger cursor = BigInteger.ZERO;
        while (true) {
            var rows = store.scan(task.getId().longValueExact(), cursor, ObjectScope.all(), UNFINISHED);
            if (rows.isEmpty()) break;
            cursor = rows.getLast().getId();
            List<IamRoleVersionApplicationItemEntity> ready = new ArrayList<>();
            for (var row : rows) {
                try {
                    if (!com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql.matches(visible, row.getAssignmentId().longValueExact()))
                        throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                    var current = domain.findAssignment(row.getAssignmentId().longValueExact());
                    if (current == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                    if (refresh) {
                        var latest = domain.snapshot(admission, current);
                        row.setOriginalSnapshot(IamJsonCodec.object(latest)); row.setExpectedAssignmentVersion(current.getVersion());
                        if (current.getRevisionId().equals(task.getTargetRevisionId())) {
                            row.setStatus(RoleVersionApplicationItemStatus.ALREADY_TARGET); row.setFailureReason(null);
                            row.setPreviewSnapshot(null); store.save(row); continue;
                        }
                    }
                    IamIds.requireVersion(row.getExpectedAssignmentVersion().toString(), current.getVersion().toString());
                    ready.add(row);
                } catch (BizException failure) { blocked = true; block(row, failure); }
            }
            if (ready.isEmpty()) continue;
            try {
                var command = new AssignmentUpgradeInput(target(task), ready.stream().map(row -> new AssignmentUpgradeItem(
                        row.getAssignmentId().toString(), row.getExpectedAssignmentVersion().toString(), reuse ? null : bindings(row))).toList());
                var result = domain.preview(admission, command);
                var previews = result.items().stream().collect(java.util.stream.Collectors.toMap(AssignmentUpgradePreviewItem::id, item -> item));
                for (var row : ready) { var preview = previews.get(row.getAssignmentId().toString()); savePreview(row, preview); blocked |= !preview.allowed(); }
            } catch (BizException batchFailure) {
                // 合法输入按百条预检。极少数结构错误以逐条定位，不能把一个错误误报到其他分配。
                for (var row : ready) {
                    try {
                        var result = domain.preview(admission, new AssignmentUpgradeInput(target(task),
                                List.of(new AssignmentUpgradeItem(row.getAssignmentId().toString(), row.getExpectedAssignmentVersion().toString(), reuse ? null : bindings(row)))));
                        var preview = result.items().getFirst(); savePreview(row, preview); blocked |= !preview.allowed();
                    } catch (BizException failure) { blocked = true; block(row, failure); }
                }
            }
        }
        task.setStatus(blocked ? RoleVersionApplicationStatus.BLOCKED : RoleVersionApplicationStatus.READY);
        task.setFailureReason(null); task.setLeaseOwner(null); task.setLeaseUntil(null);
    }

    private void savePreview(IamRoleVersionApplicationItemEntity row, AssignmentUpgradePreviewItem preview) {
        row.setConfiguredBindings(IamJsonCodec.object(preview.scopeBindings())); row.setPreviewSnapshot(IamJsonCodec.object(preview));
        row.setStatus(preview.allowed() ? RoleVersionApplicationItemStatus.PENDING : RoleVersionApplicationItemStatus.BLOCKED);
        var message = preview.issues().stream().map(ValidationIssue::message).collect(java.util.stream.Collectors.joining("；"));
        row.setFailureReason(preview.allowed() ? null : message.substring(0, Math.min(message.length(), MAX_REASON_LENGTH)));
        store.save(row);
    }
    private void block(IamRoleVersionApplicationItemEntity row, BizException failure) {
        row.setStatus(RoleVersionApplicationItemStatus.BLOCKED); row.setFailureReason(reason(failure)); row.setPreviewSnapshot(null); store.save(row);
    }

    private AssignmentUpgradeInput command(IamRoleVersionApplicationTaskEntity task, List<IamRoleVersionApplicationItemEntity> rows) {
        return new AssignmentUpgradeInput(target(task), rows.stream().map(r -> new AssignmentUpgradeItem(
                r.getAssignmentId().toString(), r.getExpectedAssignmentVersion().toString(), bindings(r))).toList());
    }
    private RoleRevisionRef target(IamRoleVersionApplicationTaskEntity task) {
        return domain.reference(task.getTargetRevisionId());
    }
    private AuthorizationAdmission governed() {
        return domain.admit();
    }
    private ObjectScope scope(AuthorizationAdmission admission) {
        return domain.scope(admission, true);
    }
    private ObjectScope readScope(AuthorizationAdmission admission) {
        return domain.scope(admission, false);
    }
    private void requireRole(String roleId, AuthorizationAdmission admission) {
        requireRole(roleId, admission, false);
    }
    private void requireRole(String roleId, AuthorizationAdmission admission, boolean fresh) {
        domain.requireRole(roleId, admission, fresh);
    }
    private void lockRole(String roleId, AuthorizationAdmission admission) {
        domain.lockRole(IamIds.require(roleId));
        requireRole(roleId, admission, true);
    }
    private void lockWorker(IamRoleVersionApplicationTaskEntity task) {
        domain.lockRole(task.getRoleId().longValueExact());
    }
    private IamRoleVersionApplicationTaskEntity requireTask(String roleId, String id, AuthorizationAdmission admission) {
        requireRole(roleId, admission); return sameRole(store.find(IamIds.require(id)), roleId);
    }
    private IamRoleVersionApplicationTaskEntity lockedTask(String roleId, String id, AuthorizationAdmission admission) {
        requireRole(roleId, admission, true); return sameRole(store.lock(IamIds.require(id)), roleId);
    }
    private IamRoleVersionApplicationTaskEntity sameRole(IamRoleVersionApplicationTaskEntity task, String roleId) {
        if (task == null || !task.getRoleId().equals(new BigInteger(roleId))) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        return task;
    }
    private RoleVersionApplicationTask replay(IamRoleVersionApplicationTaskEntity task, AuthorizationAdmission admission, String signature) {
        if (!task.getCreatedBy().toString().equals(admission.actor().context().memberId())
                || !IamJsonCodec.read(task.getSelectionSnapshot(), new TypeReference<Object>() { })
                        .equals(IamJsonCodec.read(signature, new TypeReference<Object>() { })))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return view(task, scope(admission));
    }
    private void editable(IamRoleVersionApplicationTaskEntity task, String version) {
        IamIds.requireVersion(version, task.getVersion().toString());
        if (task.getStatus() == RoleVersionApplicationStatus.RUNNING || task.getStatus() == RoleVersionApplicationStatus.SUCCEEDED)
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
    }
    private void scan(IamRoleVersionApplicationTaskEntity task, ObjectScope scope, List<RoleVersionApplicationItemStatus> states,
            Consumer<IamRoleVersionApplicationItemEntity> consumer) {
        BigInteger cursor = BigInteger.ZERO;
        while (true) {
            var rows = store.scan(task.getId().longValueExact(), cursor, scope, states);
            if (rows.isEmpty()) return;
            rows.forEach(consumer); cursor = rows.getLast().getId();
        }
    }
    private Map<String, ScopeBinding> bindings(IamRoleVersionApplicationItemEntity row) {
        var bindings = IamJsonCodec.read(row.getConfiguredBindings(), BINDINGS);
        return bindings == null ? Map.of() : bindings;
    }
    private RoleVersionApplicationItem item(IamRoleVersionApplicationItemEntity row) {
        return new RoleVersionApplicationItem(row.getId().toString(), row.getAssignmentId().toString(), row.getStatus(),
                IamJsonCodec.read(row.getOriginalSnapshot(), ORIGINAL), bindings(row), IamJsonCodec.read(row.getPreviewSnapshot(), PREVIEW),
                row.getFailureReason());
    }
    private RoleVersionApplicationTask view(IamRoleVersionApplicationTaskEntity task, ObjectScope scope) {
        return view(task, domain.findRevision(task.getTargetRevisionId().longValueExact()),
                store.counts(task.getId().longValueExact(), scope));
    }
    private RoleVersionApplicationTask view(IamRoleVersionApplicationTaskEntity task, IamRoleRevisionEntity target,
            Map<RoleVersionApplicationItemStatus, Long> counts) {
        return new RoleVersionApplicationTask(task.getId().toString(), task.getRoleId().toString(), task.getTargetRevisionId().toString(),
                task.getStatus(), task.getVersion().toString(), counts,
                task.getFailureReason(), task.getCreatedAt().toInstant(ZoneOffset.UTC), task.getUpdatedAt().toInstant(ZoneOffset.UTC),
                target.getRevision().toString(), target.getVersionName());
    }
    private static void bump(IamRoleVersionApplicationTaskEntity task) {
        task.setVersion(task.getVersion().add(BigInteger.ONE)); task.setUpdatedAt(now());
    }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static String reason(RuntimeException failure) {
        var text = failure instanceof BizException business ? business.getCode() + "：" + business.getMessage()
                : IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode();
        return text.length() <= MAX_REASON_LENGTH ? text : text.substring(0, MAX_REASON_LENGTH);
    }
}
