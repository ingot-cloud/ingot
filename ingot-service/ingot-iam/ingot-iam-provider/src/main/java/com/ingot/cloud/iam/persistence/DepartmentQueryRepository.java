package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.DepartmentMemberCount;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>查询并维护当前租户部门树，始终绑定可信 tenantId 与类型化对象范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class DepartmentQueryRepository {
    private final IamDepartmentMapper departments;
    private final IamMemberDepartmentMapper members;
    private final IamTenantMemberMapper tenantMembers;

    /**
     * 列出当前租户可见部门，按同级顺序返回全量，供组树。
     *
     * @param tenantId 已授权租户 ID
     * @param scope 已编译范围
     * @return 部门列表
     */
    public List<IamDepartmentEntity> list(long tenantId, ObjectScope scope) {
        return departments.selectList(scoped(tenantId, scope)
                .orderByAsc(IamDepartmentEntity::getSortOrder, IamDepartmentEntity::getId));
    }

    /**
     * 分页列出当前租户部门。
     *
     * @param tenantId 已授权租户 ID
     * @param scope 已编译范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 部门页
     */
    public Page<IamDepartmentEntity> page(long tenantId, ObjectScope scope, int page, int size) {
        LambdaQueryWrapper<IamDepartmentEntity> wrapper = scoped(tenantId, scope)
                .orderByAsc(IamDepartmentEntity::getSortOrder, IamDepartmentEntity::getId);
        return departments.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 读取当前租户部门。
     *
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @return 部门记录，不存在时为空
     */
    public IamDepartmentEntity find(long tenantId, long id) {
        return departments.selectOne(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 判断目标部门是否存在且落入范围。
     *
     * @param tenantId 已授权租户 ID，平台域可空
     * @param id 部门 ID
     * @param scope 已编译范围
     * @return 命中时为 true
     */
    public boolean matches(Long tenantId, long id, ObjectScope scope) {
        if (scope.coversNone()) {
            return false;
        }
        LambdaQueryWrapper<IamDepartmentEntity> wrapper = Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id))
                .eq(tenantId != null, IamDepartmentEntity::getTenantId,
                        tenantId == null ? null : BigInteger.valueOf(tenantId));
        ObjectScopeSql.restrictDepartments(wrapper, scope);
        return departments.selectCount(wrapper) > 0;
    }

    /**
     * 按同一部门范围批量检查已加载的部门，调用方必须只传入当前租户的部门 ID。
     *
     * @param tenantId 已授权租户 ID
     * @param ids 当前页部门 ID
     * @param scope 已编译范围
     * @return 落入范围的部门 ID
     */
    public Set<BigInteger> matchingIds(long tenantId, Collection<BigInteger> ids, ObjectScope scope) {
        if (ids.isEmpty() || scope.coversNone()) {
            return Set.of();
        }
        var query = Wrappers.<IamDepartmentEntity>lambdaQuery()
                .select(IamDepartmentEntity::getId)
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .in(IamDepartmentEntity::getId, ids);
        ObjectScopeSql.restrictDepartments(query, scope);
        return departments.selectList(query).stream().map(IamDepartmentEntity::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 锁定当前租户部门，调用方须处于事务中。
     *
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @return 部门记录，不存在时为空
     */
    public IamDepartmentEntity lock(long tenantId, long id) {
        return departments.lockRow(BigInteger.valueOf(tenantId), BigInteger.valueOf(id));
    }

    /**
     * 新增部门节点。
     *
     * @param row 已填充 ID 与租户的部门
     */
    public void insert(IamDepartmentEntity row) {
        departments.insert(row);
    }

    /**
     * 更新已锁定部门并递增版本。
     *
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @param parentId 新父节点，根节点为空
     * @param name 部门名称
     * @param sortOrder 同级排序
     * @param version 持锁读到的版本
     */
    public void update(long tenantId, long id, Long parentId, String name, int sortOrder, BigInteger version) {
        departments.update(Wrappers.<IamDepartmentEntity>lambdaUpdate()
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id))
                .set(IamDepartmentEntity::getParentId, parentId == null ? null : BigInteger.valueOf(parentId))
                .set(IamDepartmentEntity::getName, name)
                .set(IamDepartmentEntity::getSortOrder, sortOrder)
                .set(IamDepartmentEntity::getVersion, version.add(BigInteger.ONE)));
    }

    /**
     * 删除当前租户部门。
     *
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     */
    public void delete(long tenantId, long id) {
        departments.delete(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 统计直接子部门数量。
     *
     * @param tenantId 已授权租户 ID
     * @param parentId 父部门 ID
     * @return 子节点数
     */
    public long childCount(long tenantId, long parentId) {
        return departments.selectCount(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentEntity::getParentId, BigInteger.valueOf(parentId)));
    }

    /**
     * 统计任职数量。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentId 部门 ID
     * @return 任职数
     */
    public long memberCount(long tenantId, long departmentId) {
        return members.selectCount(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberDepartmentEntity::getDepartmentId, BigInteger.valueOf(departmentId)));
    }

    /**
     * 批量统计当前页部门的直属 ACTIVE 成员，空集合不访问数据库。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentIds 当前页部门 ID
     * @return 部门 ID 到人数；无 ACTIVE 任职的部门不出现
     */
    public Map<Long, Integer> activeMemberCounts(long tenantId, Collection<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (DepartmentMemberCount row : members.countMembersByStatus(
                BigInteger.valueOf(tenantId),
                departmentIds.stream().map(BigInteger::valueOf).toList(),
                MemberStatus.ACTIVE)) {
            if (row.departmentId() == null) {
                continue;
            }
            counts.put(row.departmentId().longValueExact(), row.memberCount() == null ? 0 : row.memberCount());
        }
        return counts;
    }

    /**
     * 统计当前租户仍在组织内的成员，口径与未指定状态的成员列表一致，不含已移出。
     *
     * @param tenantId 已授权租户 ID
     * @return 成员数
     */
    public int currentMemberCount(long tenantId) {
        Long count = tenantMembers.selectCount(Wrappers.<IamTenantMemberEntity>lambdaQuery()
                .eq(IamTenantMemberEntity::getTenantId, BigInteger.valueOf(tenantId))
                .ne(IamTenantMemberEntity::getStatus, MemberStatus.REMOVED));
        return count == null ? 0 : Math.toIntExact(count);
    }

    /**
     * 读取父部门 ID，部门不存在时为空。
     *
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @return 父部门 ID，根节点或缺失时为空
     */
    public BigInteger parentId(long tenantId, long id) {
        IamDepartmentEntity row = departments.selectOne(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .select(IamDepartmentEntity::getParentId)
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id)));
        return row == null ? null : row.getParentId();
    }

    private LambdaQueryWrapper<IamDepartmentEntity> scoped(long tenantId, ObjectScope scope) {
        LambdaQueryWrapper<IamDepartmentEntity> wrapper = Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId));
        ObjectScopeSql.restrictDepartments(wrapper, scope);
        return wrapper;
    }
}
