package com.ingot.cloud.iam.role;

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
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.RoleApplicationRepository;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.support.*;
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
public class RoleApplicationService implements RoleApplicationWork {
    private static final List<RoleApplicationItemStatus> UNFINISHED = List.of(RoleApplicationItemStatus.PENDING,
            RoleApplicationItemStatus.BLOCKED);
    private static final TypeReference<AssignmentRecord> ORIGINAL = new TypeReference<>() { };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() { };
    private static final TypeReference<AssignmentUpgradePreviewItem> PREVIEW = new TypeReference<>() { };
    private static final int MAX_REASON_LENGTH = 512;
    private final IamAccess access;
    private final RoleApplicationDomainSupport domain;
    private final RoleApplicationRepository store;
    private final TransactionTemplate transaction;
    private final IamAuditWriter audits;
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
    public RoleApplicationService(IamAccess access, RoleApplicationDomainSupport domain, RoleApplicationRepository store,
            PlatformTransactionManager manager, IamAuditWriter audits, ApplicationEventPublisher events) {
        this.access = access; this.domain = domain; this.store = store;
        this.transaction = new TransactionTemplate(manager); this.audits = audits; this.events = events;
    }

    /**
     * 为既有版本准备应用，准备幂等重放不会重新取集合。
     * @param roleId 角色ID
     * @param input 目标和集合
     * @return 已持久化计划
     */
    public RoleApplicationTask prepare(String roleId, RoleApplicationPrepareInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission);
            var existing = store.findPrepared(IamIds.require(roleId), input.idempotencyKey());
            if (existing != null) return replay(existing, admission, IamJson.object(input));
            return prepareLocked(roleId, input, governed(), IamJson.object(input));
        });
    }

    /**
     * 发布和计划原子创建；预检冲突保留新版本，分配不变。
     * @param roleId 角色ID
     * @param input 发布及集合
     * @return 发布版本ID在计划targetRevisionId中
     */
    public RoleApplicationTask publishAndPrepare(String roleId, RolePublishApplicationInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission);
            var existing = store.findPrepared(IamIds.require(roleId), input.idempotencyKey());
            if (existing != null) return replay(existing, admission, IamJson.object(input));
            var published = domain.publish(roleId, input.publish());
            var prepare = new RoleApplicationPrepareInput(published.id(), input.selection(), input.sourceRevisionIds(),
                    input.assignmentIds(), input.idempotencyKey());
            return prepareLocked(roleId, prepare, governed(), IamJson.object(input));
        });
    }

    /** 读取当前可治理集合的任务状态和计数。 */
    public RoleApplicationTask get(String roleId, String id) {
        var admission = governed();
        return view(requireTask(roleId, id, admission), readScope(admission));
    }

    /** 分页读取角色任务，不披露越权分配计数。 */
    public PageResponse<RoleApplicationTask> list(String roleId, int page, int pageSize) {
        IamPages.require(page, pageSize);
        var admission = governed(); requireRole(roleId, admission);
        var rows = store.pageTasks(IamIds.require(roleId), page, pageSize);
        var scope = readScope(admission);
        return IamPages.of(rows.getRecords().stream().map(r -> view(r, scope)).toList(), rows.getTotal(), page, pageSize);
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
        return IamPages.details(rows.getRecords().stream().map(row -> IamDetails.of(
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
    public PageResponse<RoleApplicationItem> items(String roleId, String id, int page, int pageSize) {
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
    public RoleApplicationTask configure(String roleId, String id, RoleApplicationChangeInput input) {
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
                if (row.getStatus() != RoleApplicationItemStatus.PENDING
                        && row.getStatus() != RoleApplicationItemStatus.BLOCKED
                        && row.getStatus() != RoleApplicationItemStatus.EXCLUDED) return;
                changed.add(row.getId().toString());
                if (input.scopeBindings() != null) {
                    var merged = new LinkedHashMap<>(bindings(row)); merged.putAll(input.scopeBindings());
                    row.setConfiguredBindings(IamJson.object(merged));
                }
                if (input.excluded() != null)
                    row.setStatus(input.excluded() ? RoleApplicationItemStatus.EXCLUDED : RoleApplicationItemStatus.PENDING);
                store.save(row);
            });
            if (!input.all() && (chosen.stream().distinct().count() != chosen.size() || !changed.containsAll(chosen)))
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            preflight(task, governed(), false); bump(task); store.save(task);
            return view(task, scope(admission));
        });
    }

    /** 未完成分配按最新事实重新预检；不会纳入快照后新增的分配。 */
    public RoleApplicationTask refresh(String roleId, String id, RoleApplicationRefreshInput input) {
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
    public RoleApplicationTask confirm(String roleId, String id, RoleApplicationConfirmInput input) {
        var admission = governed();
        return transaction.execute(status -> {
            lockRole(roleId, admission); var task = lockedTask(roleId, id, governed());
            if (input.idempotencyKey().equals(task.getConfirmKey())) return view(task, scope(admission));
            editable(task, input.expectedVersion());
            if (task.getStatus() != RoleApplicationStatus.READY) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            preflight(task, governed(), false);
            if (task.getStatus() == RoleApplicationStatus.READY) {
                var running = store.runningForRole(task.getRoleId());
                if (running != null && !running.getId().equals(task.getId())) throw new BizException(IamReasonCode.REVISION_CONFLICT);
                task.setStatus(RoleApplicationStatus.RUNNING);
                task.setConfirmKey(input.idempotencyKey());
                task.setActorAccountId(new BigInteger(admission.actor().context().accountId()));
                task.setActorMemberId(new BigInteger(admission.actor().context().memberId()));
                task.setLeaseOwner(null); task.setLeaseUntil(null);
                var ready = new RoleApplicationReadyEvent(task.getId(), task.getRoleId());
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
        return store.runnable(worker, after, limit).stream().map(IamRoleApplicationTaskEntity::getId).toList();
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

    private Boolean executeBatch(IamRoleApplicationTaskEntity candidate) {
        return transaction.execute(status -> {
            lockWorker(candidate); var task = store.lock(candidate.getId().longValueExact());
            if (task.getStatus() != RoleApplicationStatus.RUNNING || !worker.equals(task.getLeaseOwner())) return false;
            if (!store.claim(task.getId(), worker)) return false;
            task = store.find(task.getId().longValueExact());
            var admission = domain.restore(task); requireRole(task.getRoleId().toString(), admission, true);
            var batch = store.scan(task.getId().longValueExact(), BigInteger.ZERO, ObjectScope.all(), UNFINISHED);
            if (!batch.isEmpty()) {
                var input = command(task, batch);
                domain.apply(admission, input);
                for (var row : batch) {
                    row.setStatus(RoleApplicationItemStatus.SUCCEEDED); row.setFailureReason(null); store.save(row);
                }
            }
            if (store.scan(task.getId().longValueExact(), BigInteger.ZERO, ObjectScope.all(), UNFINISHED).isEmpty()) {
                task.setStatus(RoleApplicationStatus.SUCCEEDED); task.setLeaseOwner(null); task.setLeaseUntil(null);
            }
            bump(task); store.save(task);
            return task.getStatus() == RoleApplicationStatus.RUNNING;
        });
    }

    private boolean pause(IamRoleApplicationTaskEntity candidate, RuntimeException failure) {
        try {
            transaction.executeWithoutResult(status -> {
                lockWorker(candidate); var task = store.lock(candidate.getId().longValueExact());
                if (task.getStatus() != RoleApplicationStatus.RUNNING || !worker.equals(task.getLeaseOwner())) return;
                task.setStatus(RoleApplicationStatus.PAUSED); task.setFailureReason(reason(failure));
                var batch = store.scan(task.getId().longValueExact(), BigInteger.ZERO, ObjectScope.all(), UNFINISHED);
                for (var row : batch) {
                    row.setStatus(RoleApplicationItemStatus.BLOCKED); row.setFailureReason(reason(failure)); row.setPreviewSnapshot(null); store.save(row);
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

    private RoleApplicationTask prepareLocked(String roleId, RoleApplicationPrepareInput input,
            IamAdmission admission, String signature) {
        var target = domain.requireTarget(roleId, input.targetRevisionId());
        var sources = input.sourceRevisionIds() == null ? List.<BigInteger>of()
                : input.sourceRevisionIds().stream().map(v -> BigInteger.valueOf(IamIds.require(v))).distinct().toList();
        var selected = input.assignmentIds() == null ? List.<BigInteger>of()
                : input.assignmentIds().stream().map(v -> BigInteger.valueOf(IamIds.require(v))).distinct().toList();
        if (input.selection() == RoleApplicationSelection.SOURCE_REVISIONS && sources.isEmpty()
                || input.selection() == RoleApplicationSelection.ASSIGNMENTS && selected.isEmpty())
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        for (var source : sources) {
            var row = domain.findRevision(source.longValueExact());
            if (row == null || !row.getRoleId().equals(target.getRoleId())) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var task = new IamRoleApplicationTaskEntity();
        task.setId(BigInteger.valueOf(access.nextId())); task.setRoleId(target.getRoleId());
        task.setTargetRevisionId(target.getId()); task.setCreatedBy(new BigInteger(admission.actor().context().memberId()));
        task.setActorAccountId(new BigInteger(admission.actor().context().accountId()));
        task.setActorMemberId(task.getCreatedBy()); task.setSelection(input.selection());
        task.setSelectionSnapshot(signature); task.setPrepareKey(input.idempotencyKey());
        task.setStatus(RoleApplicationStatus.PREPARING); task.setVersion(BigInteger.ZERO);
        task.setCreatedAt(now()); task.setUpdatedAt(task.getCreatedAt()); store.insert(task);
        BigInteger cursor = BigInteger.ZERO; long selectedCount = 0;
        var scope = scope(admission);
        requireCollectionCoverage(roleId, input, sources, scope);
        while (true) {
            var rows = domain.scanAssignments(IamIds.require(roleId), cursor,
                    input.selection() == RoleApplicationSelection.SOURCE_REVISIONS ? sources : null,
                    input.selection() == RoleApplicationSelection.ASSIGNMENTS ? selected : null, scope,
                    RoleApplicationRepository.BATCH_SIZE);
            if (rows.isEmpty()) break;
            for (var assignment : rows) {
                var original = domain.snapshot(admission, assignment);
                var row = new IamRoleApplicationItemEntity(); row.setId(BigInteger.valueOf(access.nextId()));
                row.setTaskId(task.getId()); row.setAssignmentId(assignment.getId());
                row.setOriginalSnapshot(IamJson.object(original)); row.setExpectedAssignmentVersion(assignment.getVersion());
                row.setConfiguredBindings(IamJson.object(original.assignment().scopeBindings()));
                row.setStatus(assignment.getStatus() != GrantStatus.ACTIVE ? RoleApplicationItemStatus.REVOKED
                        : original.assignment().validUntil() != null && !Instant.now().isBefore(original.assignment().validUntil())
                            ? RoleApplicationItemStatus.EXPIRED
                        : assignment.getRevisionId().equals(target.getId()) ? RoleApplicationItemStatus.ALREADY_TARGET
                        : RoleApplicationItemStatus.PENDING);
                store.insert(row); selectedCount++;
            }
            cursor = rows.getLast().getId();
        }
        if (input.selection() == RoleApplicationSelection.ASSIGNMENTS && selectedCount != selected.size())
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        preflight(task, admission, false, true); store.save(task);
        audits.write(admission.actor().context(), access.nextId(), APPLICATION_AUDIT, task.getId().toString(),
                AuditChangeType.CREATE, Map.of(), Map.of(AuditField.ROLE_REVISION, task.getTargetRevisionId().toString(),
                        AuditField.RECIPIENT_SELECTION, task.getSelection().getValue()), Map.of(APPLICATION_AUDIT, task.getVersion().toString()));
        return view(task, scope);
    }

    private void requireCollectionCoverage(String roleId, RoleApplicationPrepareInput input,
            List<BigInteger> sources, ObjectScope scope) {
        if (scope.coversAll() || input.selection() == RoleApplicationSelection.ASSIGNMENTS) return;
        BigInteger cursor = BigInteger.ZERO;
        while (true) {
            var rows = domain.scanAssignments(IamIds.require(roleId), cursor,
                    input.selection() == RoleApplicationSelection.SOURCE_REVISIONS ? sources : null,
                    null, ObjectScope.all(), RoleApplicationRepository.BATCH_SIZE);
            if (rows.isEmpty()) return;
            for (var row : rows) {
                if (row.getStatus() == GrantStatus.ACTIVE
                        && (row.getValidUntil() == null || row.getValidUntil().isAfter(now()))
                        && !com.ingot.cloud.iam.persistence.ObjectScopeSql.matches(scope, row.getId().longValueExact()))
                    throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            cursor = rows.getLast().getId();
        }
    }

    private void preflight(IamRoleApplicationTaskEntity task, IamAdmission admission, boolean refresh) {
        preflight(task, admission, refresh, false);
    }

    private void preflight(IamRoleApplicationTaskEntity task, IamAdmission admission, boolean refresh, boolean reuse) {
        var visible = scope(admission);
        boolean blocked = false;
        BigInteger cursor = BigInteger.ZERO;
        while (true) {
            var rows = store.scan(task.getId().longValueExact(), cursor, ObjectScope.all(), UNFINISHED);
            if (rows.isEmpty()) break;
            cursor = rows.getLast().getId();
            List<IamRoleApplicationItemEntity> ready = new ArrayList<>();
            for (var row : rows) {
                try {
                    if (!com.ingot.cloud.iam.persistence.ObjectScopeSql.matches(visible, row.getAssignmentId().longValueExact()))
                        throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                    var current = domain.findAssignment(row.getAssignmentId().longValueExact());
                    if (current == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                    if (refresh) {
                        var latest = domain.snapshot(admission, current);
                        row.setOriginalSnapshot(IamJson.object(latest)); row.setExpectedAssignmentVersion(current.getVersion());
                        if (current.getRevisionId().equals(task.getTargetRevisionId())) {
                            row.setStatus(RoleApplicationItemStatus.ALREADY_TARGET); row.setFailureReason(null);
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
        task.setStatus(blocked ? RoleApplicationStatus.BLOCKED : RoleApplicationStatus.READY);
        task.setFailureReason(null); task.setLeaseOwner(null); task.setLeaseUntil(null);
    }

    private void savePreview(IamRoleApplicationItemEntity row, AssignmentUpgradePreviewItem preview) {
        row.setConfiguredBindings(IamJson.object(preview.scopeBindings())); row.setPreviewSnapshot(IamJson.object(preview));
        row.setStatus(preview.allowed() ? RoleApplicationItemStatus.PENDING : RoleApplicationItemStatus.BLOCKED);
        var message = preview.issues().stream().map(ValidationIssue::message).collect(java.util.stream.Collectors.joining("；"));
        row.setFailureReason(preview.allowed() ? null : message.substring(0, Math.min(message.length(), MAX_REASON_LENGTH)));
        store.save(row);
    }
    private void block(IamRoleApplicationItemEntity row, BizException failure) {
        row.setStatus(RoleApplicationItemStatus.BLOCKED); row.setFailureReason(reason(failure)); row.setPreviewSnapshot(null); store.save(row);
    }

    private AssignmentUpgradeInput command(IamRoleApplicationTaskEntity task, List<IamRoleApplicationItemEntity> rows) {
        return new AssignmentUpgradeInput(target(task), rows.stream().map(r -> new AssignmentUpgradeItem(
                r.getAssignmentId().toString(), r.getExpectedAssignmentVersion().toString(), bindings(r))).toList());
    }
    private RoleRevisionRef target(IamRoleApplicationTaskEntity task) {
        return domain.reference(task.getTargetRevisionId());
    }
    private IamAdmission governed() {
        return domain.admit();
    }
    private ObjectScope scope(IamAdmission admission) {
        return domain.scope(admission, true);
    }
    private ObjectScope readScope(IamAdmission admission) {
        return domain.scope(admission, false);
    }
    private void requireRole(String roleId, IamAdmission admission) {
        requireRole(roleId, admission, false);
    }
    private void requireRole(String roleId, IamAdmission admission, boolean fresh) {
        domain.requireRole(roleId, admission, fresh);
    }
    private void lockRole(String roleId, IamAdmission admission) {
        domain.lockRole(IamIds.require(roleId));
        requireRole(roleId, admission, true);
    }
    private void lockWorker(IamRoleApplicationTaskEntity task) {
        domain.lockRole(task.getRoleId().longValueExact());
    }
    private IamRoleApplicationTaskEntity requireTask(String roleId, String id, IamAdmission admission) {
        requireRole(roleId, admission); return sameRole(store.find(IamIds.require(id)), roleId);
    }
    private IamRoleApplicationTaskEntity lockedTask(String roleId, String id, IamAdmission admission) {
        requireRole(roleId, admission, true); return sameRole(store.lock(IamIds.require(id)), roleId);
    }
    private IamRoleApplicationTaskEntity sameRole(IamRoleApplicationTaskEntity task, String roleId) {
        if (task == null || !task.getRoleId().equals(new BigInteger(roleId))) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        return task;
    }
    private RoleApplicationTask replay(IamRoleApplicationTaskEntity task, IamAdmission admission, String signature) {
        if (!task.getCreatedBy().toString().equals(admission.actor().context().memberId())
                || !IamJson.read(task.getSelectionSnapshot(), new TypeReference<Object>() { })
                        .equals(IamJson.read(signature, new TypeReference<Object>() { })))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return view(task, scope(admission));
    }
    private void editable(IamRoleApplicationTaskEntity task, String version) {
        IamIds.requireVersion(version, task.getVersion().toString());
        if (task.getStatus() == RoleApplicationStatus.RUNNING || task.getStatus() == RoleApplicationStatus.SUCCEEDED)
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
    }
    private void scan(IamRoleApplicationTaskEntity task, ObjectScope scope, List<RoleApplicationItemStatus> states,
            Consumer<IamRoleApplicationItemEntity> consumer) {
        BigInteger cursor = BigInteger.ZERO;
        while (true) {
            var rows = store.scan(task.getId().longValueExact(), cursor, scope, states);
            if (rows.isEmpty()) return;
            rows.forEach(consumer); cursor = rows.getLast().getId();
        }
    }
    private Map<String, ScopeBinding> bindings(IamRoleApplicationItemEntity row) {
        var bindings = IamJson.read(row.getConfiguredBindings(), BINDINGS);
        return bindings == null ? Map.of() : bindings;
    }
    private RoleApplicationItem item(IamRoleApplicationItemEntity row) {
        return new RoleApplicationItem(row.getId().toString(), row.getAssignmentId().toString(), row.getStatus(),
                IamJson.read(row.getOriginalSnapshot(), ORIGINAL), bindings(row), IamJson.read(row.getPreviewSnapshot(), PREVIEW),
                row.getFailureReason());
    }
    private RoleApplicationTask view(IamRoleApplicationTaskEntity task, ObjectScope scope) {
        var target = domain.findRevision(task.getTargetRevisionId().longValueExact());
        return new RoleApplicationTask(task.getId().toString(), task.getRoleId().toString(), task.getTargetRevisionId().toString(),
                task.getStatus(), task.getVersion().toString(), store.counts(task.getId().longValueExact(), scope),
                task.getFailureReason(), task.getCreatedAt().toInstant(ZoneOffset.UTC), task.getUpdatedAt().toInstant(ZoneOffset.UTC),
                target.getRevision().toString(), target.getVersionName());
    }
    private static void bump(IamRoleApplicationTaskEntity task) {
        task.setVersion(task.getVersion().add(BigInteger.ONE)); task.setUpdatedAt(now());
    }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static String reason(RuntimeException failure) {
        var text = failure instanceof BizException business ? business.getCode() + "：" + business.getMessage()
                : IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode();
        return text.length() <= MAX_REASON_LENGTH ? text : text.substring(0, MAX_REASON_LENGTH);
    }
}
