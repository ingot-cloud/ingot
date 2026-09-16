package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
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
        LambdaQueryWrapper<IamDepartmentEntity> wrapper = Wrappers.<IamDepartmentEntity>lambdaQuery()
                .eq(IamDepartmentEntity::getId, BigInteger.valueOf(id))
                .eq(tenantId != null, IamDepartmentEntity::getTenantId,
                        tenantId == null ? null : BigInteger.valueOf(tenantId));
        ObjectScopeSql.restrictDepartments(wrapper, scope);
        return departments.selectCount(wrapper) > 0;
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
