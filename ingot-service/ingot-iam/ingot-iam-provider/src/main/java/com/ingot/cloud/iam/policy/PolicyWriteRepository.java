package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamPolicySelectorDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamPolicySelectorEntity;
import com.ingot.cloud.iam.persistence.entity.IamPolicySelectorMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDefaultPolicyRevisionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldPolicyMapper;
import com.ingot.cloud.iam.persistence.mapper.IamFieldRuleMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPolicySelectorMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护租户字段策略的读写，选择器整体替换，查询始终绑定可信 tenantId。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PolicyWriteRepository {
    private final IamFieldPolicyMapper fieldPolicies;
    private final IamFieldRuleMapper fieldRules;
    private final IamPolicySelectorMapper selectors;
    private final IamPolicySelectorMemberMapper selectorMembers;
    private final IamPolicySelectorDepartmentMapper selectorDepartments;
    private final IamTenantMemberMapper members;
    private final IamDefaultPolicyRevisionMapper defaults;

    /**
     * 读取当前租户某一资源的字段策略引用。
     *
     * @param tenantId 已授权租户 ID
     * @param resourceId 已接入资源 ID
     * @return 策略行；尚未配置时为空
     */
    public IamFieldPolicyEntity findField(long tenantId, long resourceId) {
        return fieldPolicies.selectOne(Wrappers.<IamFieldPolicyEntity>lambdaQuery()
                .select(IamFieldPolicyEntity::getTenantId, IamFieldPolicyEntity::getResourceId,
                        IamFieldPolicyEntity::getDefaultRevisionId, IamFieldPolicyEntity::getVersion,
                        IamFieldPolicyEntity::getOperationRules)
                .eq(IamFieldPolicyEntity::getTenantId, id(tenantId))
                .eq(IamFieldPolicyEntity::getResourceId, id(resourceId)));
    }

    /**
     * 按插入顺序读取当前租户某一资源的字段规则。
     *
     * @param tenantId 已授权租户 ID
     * @param resourceId 已接入资源 ID
     * @return 规则列表
     */
    public List<IamFieldRuleEntity> listFieldRules(long tenantId, long resourceId) {
        return fieldRules.selectList(Wrappers.<IamFieldRuleEntity>lambdaQuery()
                .select(IamFieldRuleEntity::getResourceId, IamFieldRuleEntity::getFieldKey,
                        IamFieldRuleEntity::getViewerSelectorId, IamFieldRuleEntity::getTargetScope,
                        IamFieldRuleEntity::getScopeBindings, IamFieldRuleEntity::getVisibility)
                .eq(IamFieldRuleEntity::getTenantId, id(tenantId))
                .eq(IamFieldRuleEntity::getResourceId, id(resourceId))
                .orderByAsc(IamFieldRuleEntity::getId));
    }

    /**
     * 删除当前租户某一资源的全部字段规则，供整体替换。
     *
     * @param tenantId 已授权租户 ID
     * @param resourceId 已接入资源 ID
     */
    public void deleteFieldRules(long tenantId, long resourceId) {
        fieldRules.delete(Wrappers.<IamFieldRuleEntity>lambdaQuery()
                .eq(IamFieldRuleEntity::getTenantId, id(tenantId))
                .eq(IamFieldRuleEntity::getResourceId, id(resourceId)));
    }

    /**
     * 插入或整体替换某一资源的字段策略引用及操作规则并递增版本。
     *
     * @param tenantId 已授权租户 ID
     * @param resourceId 已接入资源 ID
     * @param revisionId 默认策略版本 ID
     * @param operationRules 本资源操作规则 JSON 数组
     */
    public void upsertField(long tenantId, long resourceId, long revisionId, String operationRules) {
        fieldPolicies.upsert(id(tenantId), id(resourceId), id(revisionId), DefaultPolicyKind.FIELD, operationRules);
    }

    /**
     * 写入一条字段规则。
     *
     * @param id 规则 ID
     * @param tenantId 已授权租户 ID
     * @param resourceId 已接入资源 ID
     * @param fieldKey 字段键
     * @param viewerSelectorId 查看者选择器
     * @param targetScope 目标范围 JSON
     * @param scopeBindings 范围绑定 JSON
     * @param visibility 可见程度
     */
    public void insertFieldRule(long id, long tenantId, long resourceId, String fieldKey, long viewerSelectorId,
                                String targetScope, String scopeBindings, FieldVisibility visibility) {
        IamFieldRuleEntity row = new IamFieldRuleEntity();
        row.setId(id(id));
        row.setTenantId(id(tenantId));
        row.setResourceId(id(resourceId));
        row.setFieldKey(fieldKey);
        row.setViewerSelectorId(id(viewerSelectorId));
        row.setTargetScope(targetScope);
        row.setScopeBindings(scopeBindings);
        row.setVisibility(visibility);
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
                .select(IamDefaultPolicyRevisionEntity::getId, IamDefaultPolicyRevisionEntity::getResourceId,
                        IamDefaultPolicyRevisionEntity::getKind, IamDefaultPolicyRevisionEntity::getRevision,
                        IamDefaultPolicyRevisionEntity::getDefinition)
                .eq(IamDefaultPolicyRevisionEntity::getId, id(revisionId))
                .eq(IamDefaultPolicyRevisionEntity::getKind, kind));
    }

    /**
     * 写入由资源字段默认值生成的第 1 版。
     *
     * @param revision 尚未持久化的修订
     */
    public void insertRevision(IamDefaultPolicyRevisionEntity revision) {
        defaults.insert(revision);
    }

    /**
     * 为已接入资源补一条空策略行。
     *
     * @param row 策略行
     */
    public void insertField(IamFieldPolicyEntity row) {
        fieldPolicies.insert(row);
    }

    /**
     * 读取某一资源最新发布的默认策略版本。
     *
     * @param kind 策略类别
     * @param resourceId 资源 ID
     * @return 版本行；不存在时为空
     */
    public IamDefaultPolicyRevisionEntity latestRevision(DefaultPolicyKind kind, long resourceId) {
        List<IamDefaultPolicyRevisionEntity> rows = defaults.selectList(
                Wrappers.<IamDefaultPolicyRevisionEntity>lambdaQuery()
                        .select(IamDefaultPolicyRevisionEntity::getId, IamDefaultPolicyRevisionEntity::getResourceId,
                                IamDefaultPolicyRevisionEntity::getKind, IamDefaultPolicyRevisionEntity::getRevision,
                                IamDefaultPolicyRevisionEntity::getDefinition)
                        .eq(IamDefaultPolicyRevisionEntity::getKind, kind)
                        .eq(IamDefaultPolicyRevisionEntity::getResourceId, id(resourceId))
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
     * 删除该成员在当前租户全部策略选择器引用。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 被删除的选择器成员行数
     */
    public int removeSelectorMember(long tenantId, long memberId) {
        return selectorMembers.delete(Wrappers.<IamPolicySelectorMemberEntity>lambdaQuery()
                .eq(IamPolicySelectorMemberEntity::getTenantId, id(tenantId))
                .eq(IamPolicySelectorMemberEntity::getMemberId, id(memberId)));
    }

    private static BigInteger id(long value) {
        return BigInteger.valueOf(value);
    }
    /** 按数据库当前事实判定组织拥有者。 */
    public boolean currentOwner(long tenantId, long memberId) {
        try { return members.currentOwner(id(tenantId), id(memberId)) == 1; }
        catch(org.springframework.dao.DataAccessException failure) {
            throw new com.ingot.framework.commons.error.BizException(com.ingot.framework.commons.model.iam.IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
    }

}
