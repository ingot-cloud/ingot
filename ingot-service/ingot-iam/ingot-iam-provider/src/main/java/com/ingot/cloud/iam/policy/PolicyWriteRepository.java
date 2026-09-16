package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDirectoryPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamDirectoryRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPolicySelectorDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPolicySelectorEntity;
import com.ingot.cloud.iam.persistence.entity.IamPolicySelectorMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDefaultPolicyRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDirectoryPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDirectoryRuleMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldRuleMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PolicyEffect;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护租户通讯录与字段策略的读写，选择器整体替换，成员与部门查询始终绑定可信 tenantId。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PolicyWriteRepository {
    private final IamDirectoryPolicyMapper directoryPolicies;
    private final IamFieldPolicyMapper fieldPolicies;
    private final IamDirectoryRuleMapper directoryRules;
    private final IamFieldRuleMapper fieldRules;
    private final IamPolicySelectorMapper selectors;
    private final IamPolicySelectorMemberMapper selectorMembers;
    private final IamPolicySelectorDepartmentMapper selectorDepartments;
    private final IamTenantMemberMapper members;
    private final IamMemberDepartmentMapper memberships;
    private final IamDepartmentMapper departments;
    private final IamDefaultPolicyRevisionMapper defaults;

    /**
     * 读取当前租户通讯录策略引用。
     *
     * @param tenantId 已授权租户 ID
     * @return 策略行；尚未配置时为空
     */
    public IamDirectoryPolicyEntity findDirectory(long tenantId) {
        return directoryPolicies.selectOne(Wrappers.<IamDirectoryPolicyEntity>lambdaQuery()
                .select(IamDirectoryPolicyEntity::getDefaultRevisionId, IamDirectoryPolicyEntity::getDefaultScope,
                        IamDirectoryPolicyEntity::getDefaultSelectorId, IamDirectoryPolicyEntity::getVersion)
                .eq(IamDirectoryPolicyEntity::getTenantId, id(tenantId)));
    }

    /**
     * 按插入顺序读取当前租户通讯录规则。
     *
     * @param tenantId 已授权租户 ID
     * @return 规则列表
     */
    public List<IamDirectoryRuleEntity> listDirectoryRules(long tenantId) {
        return directoryRules.selectList(Wrappers.<IamDirectoryRuleEntity>lambdaQuery()
                .select(IamDirectoryRuleEntity::getEffect, IamDirectoryRuleEntity::getViewerSelectorId,
                        IamDirectoryRuleEntity::getTargetSelectorId)
                .eq(IamDirectoryRuleEntity::getTenantId, id(tenantId))
                .orderByAsc(IamDirectoryRuleEntity::getId));
    }

    /**
     * 读取当前租户字段策略引用。
     *
     * @param tenantId 已授权租户 ID
     * @return 策略行；尚未配置时为空
     */
    public IamFieldPolicyEntity findField(long tenantId) {
        return fieldPolicies.selectOne(Wrappers.<IamFieldPolicyEntity>lambdaQuery()
                .select(IamFieldPolicyEntity::getDefaultRevisionId, IamFieldPolicyEntity::getVersion)
                .eq(IamFieldPolicyEntity::getTenantId, id(tenantId)));
    }

    /**
     * 按插入顺序读取当前租户字段规则。
     *
     * @param tenantId 已授权租户 ID
     * @return 规则列表
     */
    public List<IamFieldRuleEntity> listFieldRules(long tenantId) {
        return fieldRules.selectList(Wrappers.<IamFieldRuleEntity>lambdaQuery()
                .select(IamFieldRuleEntity::getScenario, IamFieldRuleEntity::getFieldKey,
                        IamFieldRuleEntity::getViewerSelectorId, IamFieldRuleEntity::getTargetScope,
                        IamFieldRuleEntity::getScopeBindings, IamFieldRuleEntity::getVisibility,
                        IamFieldRuleEntity::getEditable)
                .eq(IamFieldRuleEntity::getTenantId, id(tenantId))
                .orderByAsc(IamFieldRuleEntity::getId));
    }

    /**
     * 删除当前租户全部通讯录规则，供整体替换。
     *
     * @param tenantId 已授权租户 ID
     */
    public void deleteDirectoryRules(long tenantId) {
        directoryRules.delete(Wrappers.<IamDirectoryRuleEntity>lambdaQuery()
                .eq(IamDirectoryRuleEntity::getTenantId, id(tenantId)));
    }

    /**
     * 插入或整体替换通讯录策略引用并递增版本。
     *
     * @param tenantId 已授权租户 ID
     * @param revisionId 默认策略版本 ID
     * @param scope 默认范围；继承固定版本时为空
     * @param selectorId SELECTED 时的选择器；其余为空
     */
    public void upsertDirectory(long tenantId, long revisionId, DirectoryDefaultScope scope, Long selectorId) {
        directoryPolicies.upsert(id(tenantId), id(revisionId), DefaultPolicyKind.DIRECTORY, scope,
                selectorId == null ? null : id(selectorId));
    }

    /**
     * 写入一条通讯录允许或禁止规则。
     *
     * @param id 规则 ID
     * @param tenantId 已授权租户 ID
     * @param effect 允许或禁止
     * @param viewerSelectorId 查看者选择器
     * @param targetSelectorId 目标选择器
     */
    public void insertDirectoryRule(long id, long tenantId, PolicyEffect effect, long viewerSelectorId,
                                    long targetSelectorId) {
        IamDirectoryRuleEntity row = new IamDirectoryRuleEntity();
        row.setId(id(id));
        row.setTenantId(id(tenantId));
        row.setEffect(effect);
        row.setViewerSelectorId(id(viewerSelectorId));
        row.setTargetSelectorId(id(targetSelectorId));
        directoryRules.insert(row);
    }

    /**
     * 删除当前租户全部字段规则，供整体替换。
     *
     * @param tenantId 已授权租户 ID
     */
    public void deleteFieldRules(long tenantId) {
        fieldRules.delete(Wrappers.<IamFieldRuleEntity>lambdaQuery()
                .eq(IamFieldRuleEntity::getTenantId, id(tenantId)));
    }

    /**
     * 插入或整体替换字段策略引用并递增版本。
     *
     * @param tenantId 已授权租户 ID
     * @param revisionId 默认策略版本 ID
     */
    public void upsertField(long tenantId, long revisionId) {
        fieldPolicies.upsert(id(tenantId), id(revisionId), DefaultPolicyKind.FIELD);
    }

    /**
     * 写入一条字段规则。
     *
     * @param id 规则 ID
     * @param tenantId 已授权租户 ID
     * @param scenario 后台或通讯录场景
     * @param fieldKey 字段键
     * @param viewerSelectorId 查看者选择器
     * @param targetScope 目标范围 JSON
     * @param scopeBindings 范围绑定 JSON
     * @param visibility 可见程度
     * @param editable 是否允许写入
     */
    public void insertFieldRule(long id, long tenantId, PolicyScenario scenario, String fieldKey, long viewerSelectorId,
                                String targetScope, String scopeBindings, FieldVisibility visibility, boolean editable) {
        IamFieldRuleEntity row = new IamFieldRuleEntity();
        row.setId(id(id));
        row.setTenantId(id(tenantId));
        row.setScenario(scenario);
        row.setFieldKey(fieldKey);
        row.setViewerSelectorId(id(viewerSelectorId));
        row.setTargetScope(targetScope);
        row.setScopeBindings(scopeBindings);
        row.setVisibility(visibility);
        row.setEditable(editable);
        fieldRules.insert(row);
    }

    /**
     * 写入策略选择器主键。
     *
     * @param id 选择器 ID
     * @param tenantId 已授权租户 ID
     */
    public void insertSelector(long id, long tenantId) {
        IamPolicySelectorEntity row = new IamPolicySelectorEntity();
        row.setId(id(id));
        row.setTenantId(id(tenantId));
        selectors.insert(row);
    }

    /**
     * 写入选择器中的成员引用。
     *
     * @param tenantId 已授权租户 ID
     * @param selectorId 选择器 ID
     * @param memberId 租户成员 ID
     */
    public void insertSelectorMember(long tenantId, long selectorId, long memberId) {
        IamPolicySelectorMemberEntity row = new IamPolicySelectorMemberEntity();
        row.setTenantId(id(tenantId));
        row.setSelectorId(id(selectorId));
        row.setMemberId(id(memberId));
        selectorMembers.insert(row);
    }

    /**
     * 写入选择器中的部门引用。
     *
     * @param tenantId 已授权租户 ID
     * @param selectorId 选择器 ID
     * @param departmentId 部门 ID
     * @param descendants 是否包含下级
     */
    public void insertSelectorDepartment(long tenantId, long selectorId, long departmentId, boolean descendants) {
        IamPolicySelectorDepartmentEntity row = new IamPolicySelectorDepartmentEntity();
        row.setTenantId(id(tenantId));
        row.setSelectorId(id(selectorId));
        row.setDepartmentId(id(departmentId));
        row.setIncludeDescendants(descendants);
        selectorDepartments.insert(row);
    }

    /**
     * 读取指定默认策略版本的定义体。
     *
     * @param revisionId 版本 ID
     * @param kind 期望类别
     * @return 版本行；类别不匹配或不存在时为空
     */
    public IamDefaultPolicyRevisionEntity findRevision(long revisionId, DefaultPolicyKind kind) {
        return defaults.selectOne(Wrappers.<IamDefaultPolicyRevisionEntity>lambdaQuery()
                .select(IamDefaultPolicyRevisionEntity::getId, IamDefaultPolicyRevisionEntity::getKind,
                        IamDefaultPolicyRevisionEntity::getRevision, IamDefaultPolicyRevisionEntity::getDefinition)
                .eq(IamDefaultPolicyRevisionEntity::getId, id(revisionId))
                .eq(IamDefaultPolicyRevisionEntity::getKind, kind));
    }

    /**
     * 读取指定类别最新发布的默认策略版本。
     *
     * @param kind 策略类别
     * @return 版本行；不存在时为空
     */
    public IamDefaultPolicyRevisionEntity latestRevision(DefaultPolicyKind kind) {
        List<IamDefaultPolicyRevisionEntity> rows = defaults.selectList(
                Wrappers.<IamDefaultPolicyRevisionEntity>lambdaQuery()
                        .select(IamDefaultPolicyRevisionEntity::getId, IamDefaultPolicyRevisionEntity::getKind,
                                IamDefaultPolicyRevisionEntity::getRevision, IamDefaultPolicyRevisionEntity::getDefinition)
                        .eq(IamDefaultPolicyRevisionEntity::getKind, kind)
                        .orderByDesc(IamDefaultPolicyRevisionEntity::getRevision, IamDefaultPolicyRevisionEntity::getId));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /**
     * 读取选择器中的成员与部门引用。
     *
     * @param tenantId 已授权租户 ID
     * @param selectorId 选择器 ID
     * @return 成员与部门选择
     */
    public Selection selector(long tenantId, long selectorId) {
        return selectors(tenantId, List.of(selectorId)).getOrDefault(selectorId, new Selection(List.of(), List.of()));
    }

    /**
     * 批量读取选择器成员与部门引用，避免规则循环逐条查询。
     *
     * @param tenantId 已授权租户 ID
     * @param selectorIds 选择器 ID
     * @return 选择器 ID 到选择内容
     */
    public Map<Long, Selection> selectors(long tenantId, Collection<Long> selectorIds) {
        Map<Long, Selection> result = new LinkedHashMap<>();
        if (selectorIds == null || selectorIds.isEmpty()) {
            return result;
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long selectorId : selectorIds) {
            if (selectorId != null) {
                unique.add(selectorId);
            }
        }
        if (unique.isEmpty()) {
            return result;
        }
        BigInteger tenant = id(tenantId);
        List<BigInteger> ids = unique.stream().map(PolicyWriteRepository::id).toList();
        Map<Long, List<String>> members = new LinkedHashMap<>();
        for (IamPolicySelectorMemberEntity row : selectorMembers.selectList(
                Wrappers.<IamPolicySelectorMemberEntity>lambdaQuery()
                        .select(IamPolicySelectorMemberEntity::getSelectorId, IamPolicySelectorMemberEntity::getMemberId)
                        .eq(IamPolicySelectorMemberEntity::getTenantId, tenant)
                        .in(IamPolicySelectorMemberEntity::getSelectorId, ids)
                        .orderByAsc(IamPolicySelectorMemberEntity::getSelectorId,
                                IamPolicySelectorMemberEntity::getMemberId))) {
            members.computeIfAbsent(row.getSelectorId().longValueExact(), key -> new ArrayList<>())
                    .add(row.getMemberId().toString());
        }
        Map<Long, List<DepartmentSelection>> departments = new LinkedHashMap<>();
        for (IamPolicySelectorDepartmentEntity row : selectorDepartments.selectList(
                Wrappers.<IamPolicySelectorDepartmentEntity>lambdaQuery()
                        .select(IamPolicySelectorDepartmentEntity::getSelectorId,
                                IamPolicySelectorDepartmentEntity::getDepartmentId,
                                IamPolicySelectorDepartmentEntity::getIncludeDescendants)
                        .eq(IamPolicySelectorDepartmentEntity::getTenantId, tenant)
                        .in(IamPolicySelectorDepartmentEntity::getSelectorId, ids)
                        .orderByAsc(IamPolicySelectorDepartmentEntity::getSelectorId,
                                IamPolicySelectorDepartmentEntity::getDepartmentId))) {
            departments.computeIfAbsent(row.getSelectorId().longValueExact(), key -> new ArrayList<>())
                    .add(new DepartmentSelection(row.getDepartmentId().toString(),
                            Boolean.TRUE.equals(row.getIncludeDescendants())));
        }
        for (Long selectorId : unique) {
            result.put(selectorId, new Selection(members.getOrDefault(selectorId, List.of()),
                    departments.getOrDefault(selectorId, List.of())));
        }
        return result;
    }

    /**
     * 列出当前租户未移出成员的 ID。
     *
     * @param tenantId 已授权租户 ID
     * @return 成员 ID
     */
    public List<Long> activeMemberIds(long tenantId) {
        return members.selectList(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                        .select(IamTenantMemberEntity::getId)
                        .eq(IamTenantMemberEntity::getTenantId, id(tenantId))
                        .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED)).stream()
                .map(row -> row.getId().longValueExact()).toList();
    }

    /**
     * 读取未移出的租户成员资料。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 成员 ID
     * @return 成员行；不存在或已移出时为空
     */
    public IamTenantMemberEntity findActiveMember(long tenantId, long memberId) {
        return members.selectOne(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .select(IamTenantMemberEntity::getId, IamTenantMemberEntity::getDisplayName,
                        IamTenantMemberEntity::getAvatar, IamTenantMemberEntity::getPhone,
                        IamTenantMemberEntity::getEmail, IamTenantMemberEntity::getStatus)
                .eq(IamTenantMemberEntity::getTenantId, id(tenantId))
                .eq(IamTenantMemberEntity::getId, id(memberId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED));
    }

    /**
     * 分页列出对查看者可见的未移出成员，条件全部下推到 SQL。
     *
     * @param tenantId 已授权租户 ID
     * @param visibilities 需同时满足的可见谓词
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param page 从 1 开始的页码
     * @param pageSize 页大小
     * @return 成员页
     */
    public Page<IamTenantMemberEntity> pageVisibleMembers(long tenantId, List<DirectoryVisibility> visibilities,
                                                          String phone, String email, int page, int pageSize) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = visibleMembers(tenantId, visibilities)
                .select(IamTenantMemberEntity::getId, IamTenantMemberEntity::getDisplayName,
                        IamTenantMemberEntity::getAvatar, IamTenantMemberEntity::getPhone,
                        IamTenantMemberEntity::getEmail, IamTenantMemberEntity::getStatus,
                        IamTenantMemberEntity::getVersion)
                .eq(phone != null && !phone.isBlank(), IamTenantMemberEntity::getPhone, phone)
                .eq(email != null && !email.isBlank(), IamTenantMemberEntity::getEmail, email)
                .orderByAsc(IamTenantMemberEntity::getId);
        return members.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 判断未移出成员是否对查看者可见，详情与列表使用同一谓词。
     *
     * @param tenantId 已授权租户 ID
     * @param visibility 可见谓词
     * @param memberId 目标成员
     * @return 可见时为 true
     */
    public boolean visibleMember(long tenantId, DirectoryVisibility visibility, long memberId) {
        return members.selectCount(visibleMembers(tenantId, List.of(visibility))
                .eq(IamTenantMemberEntity::getId, id(memberId))) > 0;
    }

    /**
     * 分页列出可见成员所在部门及必要祖先骨架。
     *
     * @param tenantId 已授权租户 ID
     * @param visibilities 需同时满足的可见谓词
     * @param page 从 1 开始的页码
     * @param pageSize 页大小
     * @return 部门节点；{@code navigationOnly} 为真的节点仅作祖先导航
     */
    public Page<DirectoryDepartmentNode> pageVisibleDepartments(long tenantId,
                                                                List<DirectoryVisibility> visibilities,
                                                                int page, int pageSize) {
        Set<Long> occupied = departmentIdsOfVisibleMembers(tenantId, visibilities);
        List<IamDepartmentEntity> tree = departments.selectList(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .select(IamDepartmentEntity::getId, IamDepartmentEntity::getParentId, IamDepartmentEntity::getName,
                        IamDepartmentEntity::getSortOrder, IamDepartmentEntity::getVersion)
                .eq(IamDepartmentEntity::getTenantId, id(tenantId))
                .orderByAsc(IamDepartmentEntity::getSortOrder, IamDepartmentEntity::getId));
        Map<Long, IamDepartmentEntity> byId = new LinkedHashMap<>();
        for (IamDepartmentEntity row : tree) {
            byId.put(row.getId().longValueExact(), row);
        }
        Set<Long> shown = new LinkedHashSet<>(occupied);
        for (Long departmentId : occupied) {
            Long current = departmentId;
            while (current != null) {
                IamDepartmentEntity row = byId.get(current);
                if (row == null) {
                    break;
                }
                shown.add(current);
                current = row.getParentId() == null ? null : row.getParentId().longValueExact();
            }
        }
        if (shown.isEmpty()) {
            Page<DirectoryDepartmentNode> empty = new Page<>(page, pageSize, 0);
            empty.setRecords(List.of());
            return empty;
        }
        Page<IamDepartmentEntity> rows = departments.selectPage(new Page<>(page, pageSize),
                Wrappers.<IamDepartmentEntity>lambdaQuery()
                        .select(IamDepartmentEntity::getId, IamDepartmentEntity::getParentId,
                                IamDepartmentEntity::getName, IamDepartmentEntity::getSortOrder,
                                IamDepartmentEntity::getVersion)
                        .eq(IamDepartmentEntity::getTenantId, id(tenantId))
                        .in(IamDepartmentEntity::getId, ids(shown))
                        .orderByAsc(IamDepartmentEntity::getSortOrder, IamDepartmentEntity::getId));
        Page<DirectoryDepartmentNode> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        List<DirectoryDepartmentNode> items = new ArrayList<>();
        for (IamDepartmentEntity row : rows.getRecords()) {
            items.add(new DirectoryDepartmentNode(row, !occupied.contains(row.getId().longValueExact())));
        }
        result.setRecords(items);
        return result;
    }

    /**
     * 读取指定部门内的成员 ID。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentIds 已展开的部门 ID；调用方保证非空
     * @return 成员 ID
     */
    public List<Long> memberIdsInDepartments(long tenantId, Collection<Long> departmentIds) {
        List<BigInteger> ids = new ArrayList<>();
        for (Long departmentId : departmentIds) {
            ids.add(id(departmentId));
        }
        return memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .select(IamMemberDepartmentEntity::getMemberId)
                        .eq(IamMemberDepartmentEntity::getTenantId, id(tenantId))
                        .in(IamMemberDepartmentEntity::getDepartmentId, ids)).stream()
                .map(row -> row.getMemberId().longValueExact()).toList();
    }

    /**
     * 判断默认策略版本是否存在且类别匹配。
     *
     * @param revisionId 策略版本 ID
     * @param kind 期望类别
     * @return 命中时为 true
     */
    public boolean hasDefaultRevision(long revisionId, DefaultPolicyKind kind) {
        return defaults.selectCount(Wrappers.<IamDefaultPolicyRevisionEntity>lambdaQuery()
                .eq(IamDefaultPolicyRevisionEntity::getId, id(revisionId))
                .eq(IamDefaultPolicyRevisionEntity::getKind, kind)) > 0;
    }

    /**
     * 读取指定类别最新发布的默认策略版本 ID。
     *
     * @param kind 策略类别
     * @return 版本 ID；不存在时为空
     */
    public String latestDefaultId(DefaultPolicyKind kind) {
        IamDefaultPolicyRevisionEntity row = latestRevision(kind);
        return row == null ? null : row.getId().toString();
    }

    private LambdaQueryWrapper<IamTenantMemberEntity> visibleMembers(long tenantId,
                                                                     List<DirectoryVisibility> visibilities) {
        LambdaQueryWrapper<IamTenantMemberEntity> wrapper = Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, id(tenantId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED);
        if (visibilities != null) {
            for (DirectoryVisibility visibility : visibilities) {
                apply(wrapper, visibility);
            }
        }
        return wrapper;
    }

    private void apply(LambdaQueryWrapper<IamTenantMemberEntity> wrapper, DirectoryVisibility visibility) {
        if (visibility == null) {
            wrapper.apply("1 = 0");
            return;
        }
        if (visibility.coversAll()) {
            List<BigInteger> excluded = ids(visibility.excluded());
            excluded.remove(id(visibility.viewerId()));
            if (!excluded.isEmpty()) {
                wrapper.notIn(IamTenantMemberEntity::getId, excluded);
            }
            return;
        }
        List<BigInteger> included = ids(visibility.included());
        if (included.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(IamTenantMemberEntity::getId, included);
    }

    private Set<Long> departmentIdsOfVisibleMembers(long tenantId, List<DirectoryVisibility> visibilities) {
        StringBuilder sql = new StringBuilder(
                "SELECT 1 FROM iam_tenant_member m WHERE m.tenant_id={0} AND m.id=iam_member_department.member_id AND m.status<>{1}");
        List<Object> args = new ArrayList<>();
        args.add(id(tenantId));
        args.add(MemberStatus.REMOVED.getValue());
        int index = 2;
        if (visibilities != null) {
            for (DirectoryVisibility visibility : visibilities) {
                if (visibility == null) {
                    return Set.of();
                }
                if (visibility.coversAll()) {
                    List<BigInteger> excluded = ids(visibility.excluded());
                    excluded.remove(id(visibility.viewerId()));
                    if (excluded.isEmpty()) {
                        continue;
                    }
                    sql.append(" AND m.id NOT IN (");
                    for (int i = 0; i < excluded.size(); i++) {
                        if (i > 0) {
                            sql.append(',');
                        }
                        sql.append('{').append(index++).append('}');
                        args.add(excluded.get(i));
                    }
                    sql.append(')');
                    continue;
                }
                List<BigInteger> included = ids(visibility.included());
                if (included.isEmpty()) {
                    return Set.of();
                }
                sql.append(" AND m.id IN (");
                for (int i = 0; i < included.size(); i++) {
                    if (i > 0) {
                        sql.append(',');
                    }
                    sql.append('{').append(index++).append('}');
                    args.add(included.get(i));
                }
                sql.append(')');
            }
        }
        return memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .select(IamMemberDepartmentEntity::getDepartmentId)
                        .eq(IamMemberDepartmentEntity::getTenantId, id(tenantId))
                        .exists(sql.toString(), args.toArray())).stream()
                .map(row -> row.getDepartmentId().longValueExact())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<BigInteger> ids(Collection<Long> values) {
        List<BigInteger> ids = new ArrayList<>();
        if (values == null) {
            return ids;
        }
        for (Long value : values) {
            ids.add(id(value));
        }
        return ids;
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
}
