package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentLeaderRepository;
import com.ingot.cloud.iam.persistence.DepartmentLeaderSql;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.projection.DepartmentLeaderRow;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>维护部门负责人组织职责，不生成权限、数据范围或会话属性。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DepartmentLeaderService {
    private static final String DEPARTMENT = "department";
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final DepartmentQueryRepository departments;
    private final DepartmentLeaderRepository leaders;
    private final MemberLifecycleRepository lifecycle;
    private final FieldAccessEvaluator fields;
    private final IamAuditWriter audits;
    private final OrganizationMemberDepartments memberDepartments;

    /** 读取受部门和成员双重范围限制的关系分页，默认20条。 */
    public DepartmentLeaderPage page(String departmentId, String keyword, int page, int size, boolean candidates) {
        return page(departmentId, keyword, page, size, candidates, null);
    }

    private DepartmentLeaderPage page(String departmentId, String keyword, int page, int size, boolean candidates, String browseDepartmentId) {
        return page(departmentId, keyword, page, size, candidates, browseDepartmentId, null);
    }

    /** 专用浏览保留目标部门维护资格，分支仅收窄候选。 */
    public DepartmentLeaderPage pickerCandidates(String departmentId, String keyword, int page, int size, com.ingot.cloud.iam.persistence.MemberPickerBranch branch) {
        return page(departmentId, keyword, page, size, true, null, branch);
    }

    private DepartmentLeaderPage page(String departmentId, String keyword, int page, int size, boolean candidates, String browseDepartmentId, com.ingot.cloud.iam.persistence.MemberPickerBranch branch) {
        IamPages.require(page, size);
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
        var department = readable(actor, departmentId);
        if (candidates) writable(actor, departmentId);
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ);
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        var snapshot = snapshot(actor);
        fields.requireOriginalLookup(snapshot, member(actor), MemberFieldKey.VALUE_DISPLAY_NAME, keyword, scope);
        var query = query(actor, department.getId(), List.of(), scope, keyword, page, size, candidates);
        if (browseDepartmentId != null && !browseDepartmentId.isBlank() && (keyword == null || keyword.isBlank())) {
            access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
            scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, IamIds.require(browseDepartmentId));
            query = new DepartmentLeaderSql.Query(query.tenantId(), query.departmentId(), query.departmentIds(), query.scope(), query.keyword(), query.size(), query.offset(), query.candidates(), id(browseDepartmentId));
        }
        if (branch != null) query = new DepartmentLeaderSql.Query(query.tenantId(), query.departmentId(), query.departmentIds(), query.scope(), query.keyword(), query.size(), query.offset(), query.candidates(), null, branch);
        var rows = leaders.page(query);
        var projected = project(actor, rows, snapshot, false);
        var summary = summaries(actor, List.of(department)).get(departmentId);
        return new DepartmentLeaderPage(projected, leaders.count(query), page, size, department.getVersion().toString(),
                summary.restricted(), summary.primaryLeader());
    }

    /** 详情已验证部门读取，首屏复用同次可见摘要；缺少成员读取资格不查询人员。 */
    DepartmentLeaderPage initialPage(ActiveIdentity actor, IamDepartmentEntity department, DepartmentLeaderSummary summary) {
        if (department.getParentId() == null
                || !access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)) return null;
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        var rows = leaders.page(query(actor, department.getId(), List.of(), scope, null,
                IamPages.DEFAULT_PAGE, IamPages.DEFAULT_SIZE, false));
        return new DepartmentLeaderPage(project(actor, rows, snapshot(actor), false), summary.visibleCount(),
                IamPages.DEFAULT_PAGE, IamPages.DEFAULT_SIZE, department.getVersion().toString(),
                summary.restricted(), summary.primaryLeader());
    }

    /** 浏览候选部门只收窄原候选池，不替换正在维护的目标部门授权。 */
    public DepartmentLeaderPage candidates(String departmentId, String keyword, int page, int size, String browseDepartmentId) {
        return page(departmentId, keyword, page, size, true, browseDepartmentId);
    }

    /** 为已经授权读取的部门一次加载摘要；不从受限关系推断完整人数。 */
    public Map<String, DepartmentLeaderSummary> summaries(ActiveIdentity actor, List<IamDepartmentEntity> rows) {
        var ids = rows.stream().filter(row -> row.getParentId() != null).map(IamDepartmentEntity::getId).toList();
        if (ids.isEmpty()) return Map.of();
        Map<String, DepartmentLeaderSummary> result = new LinkedHashMap<>();
        if (!access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)) {
            ids.forEach(id -> result.put(id.toString(), new DepartmentLeaderSummary(null, 0, true)));
            return result;
        }
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        var visible = leaders.summaries(query(actor, null, ids, scope, null, 1, IamPages.DEFAULT_SIZE, false));
        var all = leaders.summaries(query(actor, null, ids, ObjectScope.all(), null, 1, IamPages.DEFAULT_SIZE, false));
        Map<BigInteger, Long> totals = all.stream().collect(Collectors.toMap(DepartmentLeaderRow::getDepartmentId, DepartmentLeaderRow::getVisibleCount));
        var primaryRows = visible.stream().filter(row -> row.getMemberId() != null).toList();
        Map<String, DepartmentLeaderRecord> primaries = project(actor, primaryRows, snapshot(actor), true).stream()
                .collect(Collectors.toMap(DepartmentLeaderRecord::memberId, value -> value, (first, ignored) -> first));
        ids.forEach(id -> result.put(id.toString(), new DepartmentLeaderSummary(null, 0, totals.getOrDefault(id, 0L) > 0)));
        visible.forEach(row -> result.put(row.getDepartmentId().toString(), new DepartmentLeaderSummary(
                row.getMemberId() == null ? null : primaries.get(row.getMemberId().toString()), row.getVisibleCount(),
                row.getVisibleCount() < totals.getOrDefault(row.getDepartmentId(), 0L))));
        return result;
    }

    /** 在组织/部门/成员锁下重验当前权限和版本，并与审计原子提交差量。 */
    @Transactional(rollbackFor = Exception.class)
    public CreatedResource update(String departmentId, DepartmentLeaderUpdateInput input) {
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_LEADERS);
        long tenantId = tenant(actor);
        if (lifecycle.lockOwner(tenantId) == null) throw new BizException(IamReasonCode.IDENTITY_INVALID);
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_LEADERS);
        var department = departments.lock(tenantId, IamIds.require(departmentId));
        if (department == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        return applyLocked(actor, departmentId, input, department, true);
    }

    /** 已持有组织与部门锁时保存负责人差量，外层统一推进版本。 */
    boolean editLocked(ActiveIdentity actor, IamDepartmentEntity department, DepartmentLeaderChanges changes) {
        var result = applyLocked(actor, department.getId().toString(), new DepartmentLeaderUpdateInput(
                department.getVersion().toString(), changes.addMemberIds(), changes.removeMemberIds(), changes.primaryMemberId()), department, false);
        return !result.version().equals(department.getVersion().toString());
    }

    private CreatedResource applyLocked(ActiveIdentity actor, String departmentId, DepartmentLeaderUpdateInput input,
                                         IamDepartmentEntity department, boolean advanceVersion) {
        long tenantId = tenant(actor);
        requireOrdinary(department);
        readable(actor, departmentId);
        IamIds.requireVersion(input.expectedVersion(), department.getVersion().toString());
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ);
        ObjectScope scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        Map<BigInteger, Boolean> before = new LinkedHashMap<>();
        leaders.lockRelations(BigInteger.valueOf(tenantId), department.getId())
                .forEach(row -> before.put(row.getMemberId(), row.isPrimaryFlag()));
        var adds = ids(input.addMemberIds());
        var removes = ids(input.removeMemberIds());
        if (!Collections.disjoint(adds, removes) || adds.stream().anyMatch(before::containsKey)
                || !before.keySet().containsAll(removes)) invalid();
        Map<BigInteger, Boolean> after = new LinkedHashMap<>(before);
        removes.forEach(after::remove);
        adds.forEach(id -> after.put(id, false));
        BigInteger previousPrimary = primary(before);
        BigInteger nextPrimary = input.primaryMemberId() == null ? primary(after) : id(input.primaryMemberId());
        if (after.isEmpty()) nextPrimary = null;
        if (!after.isEmpty() && (nextPrimary == null || !after.containsKey(nextPrimary))) invalid();
        Set<BigInteger> touched = new TreeSet<>(adds);
        touched.addAll(removes);
        boolean changedPrimary = !Objects.equals(previousPrimary, nextPrimary);
        if (!adds.isEmpty() || !removes.isEmpty() || changedPrimary) writable(actor, departmentId);
        if (changedPrimary && previousPrimary != null) touched.add(previousPrimary);
        if (changedPrimary && nextPrimary != null) touched.add(nextPrimary);
        for (var memberId : touched) {
            var locked = leaders.lockMember(BigInteger.valueOf(tenantId), memberId);
            if (locked == null || !leaders.visibleMember(BigInteger.valueOf(tenantId), memberId, scope)) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            if ((adds.contains(memberId) || changedPrimary && memberId.equals(nextPrimary)) && !locked.isAvailable()) invalid();
        }
        BigInteger selectedPrimary = nextPrimary;
        after.replaceAll((memberId, ignored) -> memberId.equals(selectedPrimary));
        if (before.equals(after)) return new CreatedResource(departmentId, department.getVersion().toString());
        writable(actor, departmentId);
        if (advanceVersion) leaders.save(BigInteger.valueOf(tenantId), department.getId(), before, after, department.getVersion());
        else leaders.save(BigInteger.valueOf(tenantId), department.getId(), before, after, department.getVersion(), false);
        String version = department.getVersion().add(BigInteger.ONE).toString();
        audits.write(actor.context(), access.nextId(), DEPARTMENT, departmentId, AuditChangeType.UPDATE,
                changedPrimary && previousPrimary != null ? Map.of(AuditField.DEPARTMENT_LEADERS, previousPrimary.toString()) : Map.of(),
                Map.of(AuditField.DEPARTMENT_LEADERS, new Change(text(adds), text(removes),
                        changedPrimary && selectedPrimary != null ? selectedPrimary.toString() : null)), Map.of(DEPARTMENT, version));
        return new CreatedResource(departmentId, version);
    }

    private IamDepartmentEntity readable(ActiveIdentity actor, String departmentId) {
        scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, IamIds.require(departmentId));
        var department = departments.find(tenant(actor), IamIds.require(departmentId));
        if (department == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        requireOrdinary(department);
        return department;
    }

    private void writable(ActiveIdentity actor, String departmentId) {
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_LEADERS);
        scopes.requireDepartmentWrite(actor.context(), IamAction.TENANT_DEPARTMENT_LEADERS, IamIds.require(departmentId));
    }

    private FieldPolicySnapshot snapshot(ActiveIdentity actor) { return fields.snapshotFresh(tenant(actor), PolicyScenario.MANAGEMENT); }

    private List<DepartmentLeaderRecord> project(ActiveIdentity actor, List<DepartmentLeaderRow> rows,
                                                FieldPolicySnapshot snapshot, boolean primary) {
        if (rows.isEmpty()) return List.of();
        var views = memberDepartments.visible(actor, rows.stream().map(DepartmentLeaderRow::getMemberId).toList());
        var batch = fields.prepare(snapshot, member(actor), rows.stream().map(row -> row.getMemberId().longValueExact()).toList());
        return rows.stream().map(row -> {
            var raw = new MemberRecord(row.getMemberId().toString(), row.getDisplayName(), null, null, null, null,
                    row.getStatus(), List.of());
            var projected = fields.project(raw, fields.memberAccess(batch, row.getMemberId().longValueExact()),
                    BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, snapshot.masks());
            return new DepartmentLeaderRecord(raw.id(), projected.displayName(), row.getStatus(),
                    primary || row.isPrimaryFlag(), row.isAvailable(), views.getOrDefault(row.getMemberId(), List.of()));
        }).toList();
    }

    private static DepartmentLeaderSql.Query query(ActiveIdentity actor, BigInteger department, List<BigInteger> ids,
                                                  ObjectScope scope, String keyword, int page, int size, boolean candidates) {
        String contains = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return new DepartmentLeaderSql.Query(BigInteger.valueOf(tenant(actor)), department, ids, scope, contains,
                size, IamPages.offset(page, size), candidates);
    }

    private static Set<BigInteger> ids(List<String> values) {
        if (values == null) { invalid(); return Set.of(); }
        Set<BigInteger> result = values.stream().map(DepartmentLeaderService::id).collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.size() != values.size()) invalid();
        return result;
    }

    private static BigInteger id(String value) { return BigInteger.valueOf(IamIds.require(value)); }
    private static long tenant(ActiveIdentity actor) { return IamIds.require(actor.context().tenantId()); }
    private static long member(ActiveIdentity actor) { return IamIds.require(actor.context().memberId()); }
    private static List<String> text(Set<BigInteger> values) { return values.stream().map(BigInteger::toString).toList(); }
    private static BigInteger primary(Map<BigInteger, Boolean> values) {
        return values.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).findFirst().orElse(null);
    }
    private static void requireOrdinary(IamDepartmentEntity department) { if (department.getParentId() == null) invalid(); }
    private static void invalid() { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }

    /**
     * <p>仅审计操作者已获知的本次关系差量，不泄露其他受限负责人。</p>
     * @author jy
     * @since 1.0.0
     */
    private record Change(List<String> addedMemberIds, List<String> removedMemberIds, String primaryMemberId) { }
}
