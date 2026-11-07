package com.ingot.cloud.iam.infrastructure.persistence;

import java.util.Collection;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationTaskEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationItemEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleVersionApplicationTaskMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleVersionApplicationItemMapper;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationStatus;
import com.ingot.cloud.iam.domain.roleapplication.RoleVersionApplicationItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>持久计划、明细和数据库租约；全部明细读取与计数使用分配对象范围。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class RoleVersionApplicationRepository {
    /** 每次处理的最大分配数。 */
    public static final int BATCH_SIZE = 100;
    /** 领取候选查询页大小，独立于分配事务批次。 */
    public static final int DISCOVERY_PAGE_SIZE = 100;
    private static final int LEASE_SECONDS = 30;
    private static final String LEASE_DUE = "lease_until IS NULL OR lease_until<=CURRENT_TIMESTAMP(6)";
    private final IamRoleVersionApplicationTaskMapper tasks;
    private final IamRoleVersionApplicationItemMapper items;

    /** 按角色、幂等键查询准备结果。 */
    public IamRoleVersionApplicationTaskEntity findPrepared(long roleId, String key) {
        return tasks.selectOne(Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaQuery()
                .eq(IamRoleVersionApplicationTaskEntity::getRoleId, BigInteger.valueOf(roleId))
                .eq(IamRoleVersionApplicationTaskEntity::getPrepareKey, key));
    }

    /**
     * 当前角色正在执行的唯一任务，调用者先持有角色锁。
     * @param roleId 角色
     * @return 运行任务；无时为空
     */
    public IamRoleVersionApplicationTaskEntity runningForRole(BigInteger roleId) {
        return tasks.selectOne(Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaQuery()
                .eq(IamRoleVersionApplicationTaskEntity::getRoleId, roleId)
                .eq(IamRoleVersionApplicationTaskEntity::getStatus, RoleVersionApplicationStatus.RUNNING));
    }

    /** 读取任务；仅服务端治理校验后调用。 */
    public IamRoleVersionApplicationTaskEntity find(long id) { return tasks.selectById(BigInteger.valueOf(id)); }

    /** 角色锁之后锁定任务。 */
    public IamRoleVersionApplicationTaskEntity lock(long id) {
        return tasks.selectOne(Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaQuery()
                .eq(IamRoleVersionApplicationTaskEntity::getId, BigInteger.valueOf(id)).last("FOR UPDATE"));
    }

    /** 保存新任务。 */
    public void insert(IamRoleVersionApplicationTaskEntity row) { tasks.insert(row); }
    /** 保存新明细。 */
    public void insert(IamRoleVersionApplicationItemEntity row) { items.insert(row); }
    /** 保存已锁定任务的状态。 */
    public void save(IamRoleVersionApplicationTaskEntity row) {
        tasks.update(null, Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaUpdate()
                .eq(IamRoleVersionApplicationTaskEntity::getId, row.getId())
                .set(IamRoleVersionApplicationTaskEntity::getStatus, row.getStatus())
                .set(IamRoleVersionApplicationTaskEntity::getVersion, row.getVersion())
                .set(IamRoleVersionApplicationTaskEntity::getActorAccountId, row.getActorAccountId())
                .set(IamRoleVersionApplicationTaskEntity::getActorMemberId, row.getActorMemberId())
                .set(IamRoleVersionApplicationTaskEntity::getConfirmKey, row.getConfirmKey())
                .set(IamRoleVersionApplicationTaskEntity::getLeaseOwner, row.getLeaseOwner())
                .set(IamRoleVersionApplicationTaskEntity::getLeaseUntil, row.getLeaseUntil())
                .set(IamRoleVersionApplicationTaskEntity::getFailureReason, row.getFailureReason())
                .set(IamRoleVersionApplicationTaskEntity::getUpdatedAt, row.getUpdatedAt()));
    }
    /** 保存明细；调用者持有任务锁。 */
    public void save(IamRoleVersionApplicationItemEntity row) {
        items.update(null, Wrappers.<IamRoleVersionApplicationItemEntity>lambdaUpdate()
                .eq(IamRoleVersionApplicationItemEntity::getId, row.getId())
                .set(IamRoleVersionApplicationItemEntity::getOriginalSnapshot, row.getOriginalSnapshot())
                .set(IamRoleVersionApplicationItemEntity::getExpectedAssignmentVersion, row.getExpectedAssignmentVersion())
                .set(IamRoleVersionApplicationItemEntity::getConfiguredBindings, row.getConfiguredBindings())
                .set(IamRoleVersionApplicationItemEntity::getPreviewSnapshot, row.getPreviewSnapshot())
                .set(IamRoleVersionApplicationItemEntity::getStatus, row.getStatus())
                .set(IamRoleVersionApplicationItemEntity::getFailureReason, row.getFailureReason()));
    }

    /** 平台角色的任务记录分页。 */
    public Page<IamRoleVersionApplicationTaskEntity> pageTasks(long roleId, int page, int size) {
        return tasks.selectPage(new Page<>(page, size), Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaQuery()
                .eq(IamRoleVersionApplicationTaskEntity::getRoleId, BigInteger.valueOf(roleId))
                .orderByDesc(IamRoleVersionApplicationTaskEntity::getId));
    }

    /** 按同一可见性边界分页明细。 */
    public Page<IamRoleVersionApplicationItemEntity> pageItems(long taskId, int page, int size, ObjectScope scope) {
        var query = query(taskId, scope).orderByAsc(IamRoleVersionApplicationItemEntity::getId);
        return items.selectPage(new Page<>(page, size), query);
    }

    /** 明细游标；用于预检、配置和执行，不一次加载全集。 */
    public List<IamRoleVersionApplicationItemEntity> scan(long taskId, BigInteger after, ObjectScope scope,
            List<RoleVersionApplicationItemStatus> statuses) {
        var query = query(taskId, scope).gt(IamRoleVersionApplicationItemEntity::getId, after);
        if (statuses != null) query.in(IamRoleVersionApplicationItemEntity::getStatus, statuses);
        return items.selectList(query.orderByAsc(IamRoleVersionApplicationItemEntity::getId).last("LIMIT " + BATCH_SIZE));
    }

    /** 同一可见性边界的分状态计数。 */
    public Map<RoleVersionApplicationItemStatus, Long> counts(long taskId, ObjectScope scope) {
        return counts(List.of(BigInteger.valueOf(taskId)), scope).get(BigInteger.valueOf(taskId));
    }

    /**
     * 批量聚合同一可见分配范围内的状态计数，缺失状态补零，空任务或空范围不查库。
     * @param taskIds 当前页任务 ID
     * @param scope 与明细读取一致的分配范围
     * @return 每个任务完整的七状态计数
     */
    public Map<BigInteger, Map<RoleVersionApplicationItemStatus, Long>> counts(
            Collection<BigInteger> taskIds, ObjectScope scope) {
        Map<BigInteger, Map<RoleVersionApplicationItemStatus, Long>> result = new LinkedHashMap<>();
        for (var taskId : taskIds) {
            Map<RoleVersionApplicationItemStatus, Long> states = new LinkedHashMap<>();
            for (var state : RoleVersionApplicationItemStatus.values()) states.put(state, 0L);
            result.put(taskId, states);
        }
        if (result.isEmpty()) return Map.of();
        var ids = scope.coversAll() ? null : scope.clauses().stream()
                .flatMap(c -> c.requiredIds().stream()).distinct().toList();
        if (ids == null || !ids.isEmpty()) {
            for (var row : items.countByTaskAndStatus(result.keySet(), ids)) {
                result.get(row.taskId()).put(row.status(), row.total());
            }
        }
        Map<BigInteger, Map<RoleVersionApplicationItemStatus, Long>> immutable = new LinkedHashMap<>();
        result.forEach((id, states) -> immutable.put(id, Map.copyOf(states)));
        return Map.copyOf(immutable);
    }

    /** 数据库时间确定可领取任务；持有租约的本节点可继续执行下一批。 */
    public List<IamRoleVersionApplicationTaskEntity> runnable(String owner) {
        return runnable(owner, BigInteger.ZERO, DISCOVERY_PAGE_SIZE);
    }

    /**
     * 游标读取轻量领取提示，不加载集合或失败详情JSON。
     * @param owner 当前节点
     * @param after 已扫描的任务ID，不包含边界
     * @param limit 本页上限，不超过领取页大小
     * @return 当前可领取的任务标识和锁定所需角色
     */
    public List<IamRoleVersionApplicationTaskEntity> runnable(String owner, BigInteger after, int limit) {
        if (limit <= 0 || limit > DISCOVERY_PAGE_SIZE) throw new IllegalArgumentException("Invalid discovery page size");
        return tasks.selectList(Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaQuery()
                .select(IamRoleVersionApplicationTaskEntity::getId, IamRoleVersionApplicationTaskEntity::getRoleId)
                .eq(IamRoleVersionApplicationTaskEntity::getStatus, RoleVersionApplicationStatus.RUNNING)
                .gt(IamRoleVersionApplicationTaskEntity::getId, after)
                .and(q -> q.eq(IamRoleVersionApplicationTaskEntity::getLeaseOwner, owner).or().apply(LEASE_DUE))
                .orderByAsc(IamRoleVersionApplicationTaskEntity::getId).last("LIMIT " + limit));
    }

    /** 按主键检查当前领取资格，通知不触发集合扫描。 */
    public IamRoleVersionApplicationTaskEntity runnableTask(BigInteger id, String owner) {
        return tasks.selectOne(Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaQuery()
                .select(IamRoleVersionApplicationTaskEntity::getId, IamRoleVersionApplicationTaskEntity::getRoleId)
                .eq(IamRoleVersionApplicationTaskEntity::getId, id)
                .eq(IamRoleVersionApplicationTaskEntity::getStatus, RoleVersionApplicationStatus.RUNNING)
                .and(q -> q.eq(IamRoleVersionApplicationTaskEntity::getLeaseOwner, owner).or().apply(LEASE_DUE)));
    }

    /** 在任务行锁内原子领取租约；其他节点的未到期租约不允许覆盖。 */
    public boolean claim(BigInteger id, String owner) {
        return tasks.update(null, Wrappers.<IamRoleVersionApplicationTaskEntity>lambdaUpdate()
                .eq(IamRoleVersionApplicationTaskEntity::getId, id)
                .eq(IamRoleVersionApplicationTaskEntity::getStatus, RoleVersionApplicationStatus.RUNNING)
                .and(q -> q.eq(IamRoleVersionApplicationTaskEntity::getLeaseOwner, owner).or().apply(LEASE_DUE))
                .set(IamRoleVersionApplicationTaskEntity::getLeaseOwner, owner)
                .setSql("lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL " + LEASE_SECONDS + " SECOND)")) == 1;
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IamRoleVersionApplicationItemEntity> query(
            long taskId, ObjectScope scope) {
        var query = Wrappers.<IamRoleVersionApplicationItemEntity>lambdaQuery()
                .eq(IamRoleVersionApplicationItemEntity::getTaskId, BigInteger.valueOf(taskId));
        if (!scope.coversAll()) {
            var ids = scope.clauses().stream().flatMap(c -> c.requiredIds().stream()).distinct().toList();
            if (ids.isEmpty()) query.apply("1=0");
            else query.in(IamRoleVersionApplicationItemEntity::getAssignmentId, ids);
        }
        return query;
    }
}
