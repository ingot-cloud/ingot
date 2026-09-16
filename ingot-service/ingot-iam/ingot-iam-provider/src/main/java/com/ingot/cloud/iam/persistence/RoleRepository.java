package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleParameterEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleAssignmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDefinitionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleParameterMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleRevisionMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>维护角色定义、不可变版本及其授权、差异与参数行，目录可见性由调用方传入的域与租户限定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class RoleRepository {
    private final IamRoleDefinitionMapper definitions;
    private final IamRoleRevisionMapper revisions;
    private final IamRoleGrantMapper grants;
    private final IamRoleDeltaMapper deltas;
    private final IamRoleParameterMapper parameters;
    private final IamRoleAssignmentMapper assignments;
    private final IamActionMapper actions;

    /**
     * 分页列出当前入口可见的角色定义。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param tenantId 租户入口的可信组织 ID，其余入口忽略
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 角色页
     */
    public Page<IamRoleDefinitionEntity> pageDefinitions(AuthorizationDomain domain, boolean shared, Long tenantId,
                                                         int page, int pageSize) {
        return definitions.selectPage(new Page<>(page, pageSize),
                visible(domain, shared, tenantId).orderByAsc(IamRoleDefinitionEntity::getId));
    }

    /**
     * 读取当前入口可见的角色定义。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param tenantId 租户入口的可信组织 ID，其余入口忽略
     * @param id 角色 ID
     * @return 角色行，不存在或不在可见范围时为空
     */
    public IamRoleDefinitionEntity findDefinition(AuthorizationDomain domain, boolean shared, Long tenantId, long id) {
        List<IamRoleDefinitionEntity> rows = definitions.selectList(visible(domain, shared, tenantId)
                .eq(IamRoleDefinitionEntity::getId, BigInteger.valueOf(id)));
        return rows.size() == 1 ? rows.getFirst() : null;
    }

    /**
     * 锁定当前入口可见的角色定义，调用方须处于事务中。
     *
     * @param domain 接口管理域
     * @param shared 是否共享角色入口
     * @param tenantId 租户入口的可信组织 ID，其余入口忽略
     * @param id 角色 ID
     * @return 锁定行，不存在或不在可见范围时为空
     */
    public IamRoleDefinitionEntity lockDefinition(AuthorizationDomain domain, boolean shared, Long tenantId, long id) {
        BigInteger roleId = BigInteger.valueOf(id);
        if (shared) {
            return definitions.lockShared(roleId, RoleKind.SHARED);
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return definitions.lockPlatform(roleId, domain, RoleKind.SYSTEM, RoleKind.PLATFORM_CUSTOM);
        }
        return definitions.lockTenant(roleId, domain, BigInteger.valueOf(tenantKey(tenantId)), RoleKind.SYSTEM,
                RoleKind.SHARED, RoleKind.TENANT_CUSTOM);
    }

    /**
     * 插入角色定义，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertDefinition(IamRoleDefinitionEntity entity) {
        definitions.insert(entity);
    }

    /**
     * 更新启停状态并将版本加一。
     *
     * @param id 角色 ID
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updateEnabled(long id, boolean enabled, BigInteger currentVersion) {
        definitions.update(Wrappers.<IamRoleDefinitionEntity>lambdaUpdate()
                .eq(IamRoleDefinitionEntity::getId, BigInteger.valueOf(id))
                .set(IamRoleDefinitionEntity::getEnabled, enabled)
                .set(IamRoleDefinitionEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 将角色定义版本加一。
     *
     * @param id 角色 ID
     * @param currentVersion 锁定后的当前版本
     */
    public void incrementVersion(long id, BigInteger currentVersion) {
        definitions.update(Wrappers.<IamRoleDefinitionEntity>lambdaUpdate()
                .eq(IamRoleDefinitionEntity::getId, BigInteger.valueOf(id))
                .set(IamRoleDefinitionEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除角色定义。
     *
     * @param id 角色 ID
     */
    public void deleteDefinition(long id) {
        definitions.deleteById(BigInteger.valueOf(id));
    }

    /**
     * 统计引用该角色任一版本的授权条数。
     *
     * @param roleId 角色 ID
     * @return 授权条数
     */
    public long countAssignments(long roleId) {
        return assignments.countByRoleId(BigInteger.valueOf(roleId));
    }

    /**
     * 删除角色全部版本的参数、授权、差异与版本行。
     *
     * @param roleId 角色 ID
     */
    public void deleteRevisions(long roleId) {
        BigInteger id = BigInteger.valueOf(roleId);
        parameters.deleteByRoleId(id);
        grants.deleteByRoleId(id);
        deltas.deleteByRoleId(id);
        revisions.delete(Wrappers.<IamRoleRevisionEntity>lambdaQuery().eq(IamRoleRevisionEntity::getRoleId, id));
    }

    /**
     * 分页列出角色不可变版本，新版本在前。
     *
     * @param roleId 角色 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @return 版本页
     */
    public Page<IamRoleRevisionEntity> pageRevisions(long roleId, int page, int pageSize) {
        return revisions.selectPage(new Page<>(page, pageSize), Wrappers.<IamRoleRevisionEntity>lambdaQuery()
                .eq(IamRoleRevisionEntity::getRoleId, BigInteger.valueOf(roleId))
                .orderByDesc(IamRoleRevisionEntity::getRevision)
                .orderByDesc(IamRoleRevisionEntity::getId));
    }

    /**
     * 读取角色已发布的最大版本序号，尚无版本时为 0。
     *
     * @param roleId 角色 ID
     * @return 最大版本序号
     */
    public long maxRevision(long roleId) {
        IamRoleRevisionEntity latest = latestRevision(roleId);
        return latest == null || latest.getRevision() == null ? 0L : latest.getRevision().longValue();
    }

    /**
     * 读取角色最新不可变版本。
     *
     * @param roleId 角色 ID
     * @return 最新版本行，尚无版本时为空
     */
    public IamRoleRevisionEntity latestRevision(long roleId) {
        Page<IamRoleRevisionEntity> page = revisions.selectPage(new Page<>(1, 1, false),
                Wrappers.<IamRoleRevisionEntity>lambdaQuery()
                        .eq(IamRoleRevisionEntity::getRoleId, BigInteger.valueOf(roleId))
                        .orderByDesc(IamRoleRevisionEntity::getRevision)
                        .orderByDesc(IamRoleRevisionEntity::getId));
        return page.getRecords().isEmpty() ? null : page.getRecords().getFirst();
    }

    /**
     * 按主键读取角色版本。
     *
     * @param revisionId 版本 ID
     * @return 版本行，不存在时为空
     */
    public IamRoleRevisionEntity findRevision(long revisionId) {
        return revisions.selectById(BigInteger.valueOf(revisionId));
    }

    /**
     * 读取版本的完整授权行。
     *
     * @param revisionId 版本 ID
     * @return 授权行
     */
    public List<IamRoleGrantEntity> listGrants(long revisionId) {
        return grants.selectList(Wrappers.<IamRoleGrantEntity>lambdaQuery()
                .eq(IamRoleGrantEntity::getRevisionId, BigInteger.valueOf(revisionId)));
    }

    /**
     * 读取版本的租户差异行。
     *
     * @param revisionId 版本 ID
     * @return 差异行
     */
    public List<IamRoleDeltaEntity> listDeltas(long revisionId) {
        return deltas.selectList(Wrappers.<IamRoleDeltaEntity>lambdaQuery()
                .eq(IamRoleDeltaEntity::getRevisionId, BigInteger.valueOf(revisionId)));
    }

    /**
     * 读取版本的参数定义行。
     *
     * @param revisionId 版本 ID
     * @return 参数行
     */
    public List<IamRoleParameterEntity> listParameters(long revisionId) {
        return parameters.selectList(Wrappers.<IamRoleParameterEntity>lambdaQuery()
                .eq(IamRoleParameterEntity::getRevisionId, BigInteger.valueOf(revisionId)));
    }

    /**
     * 插入角色版本，不写生成列。
     *
     * @param entity 待插入行
     */
    public void insertRevision(IamRoleRevisionEntity entity) {
        revisions.insert(entity);
    }

    /**
     * 插入版本参数定义。
     *
     * @param entity 待插入行
     */
    public void insertParameter(IamRoleParameterEntity entity) {
        parameters.insert(entity);
    }

    /**
     * 插入版本授权。
     *
     * @param entity 待插入行
     */
    public void insertGrant(IamRoleGrantEntity entity) {
        grants.insert(entity);
    }

    /**
     * 插入版本差异。
     *
     * @param entity 待插入行
     */
    public void insertDelta(IamRoleDeltaEntity entity) {
        deltas.insert(entity);
    }

    /**
     * 锁定当前租户的授权行，调用方须处于事务中。
     *
     * @param tenantId 已授权租户 ID
     * @param assignmentId 授权 ID
     * @return 锁定行，不属于本租户或不存在时为空
     */
    public IamRoleAssignmentEntity lockTenantAssignment(long tenantId, long assignmentId) {
        return assignments.lockTenant(BigInteger.valueOf(assignmentId), AuthorizationDomain.TENANT,
                BigInteger.valueOf(tenantId));
    }

    /**
     * 判断角色版本是否属于指定角色。
     *
     * @param revisionId 角色版本 ID
     * @param roleId 角色定义 ID
     * @return 属于时为 true
     */
    public boolean revisionBelongsToRole(long revisionId, long roleId) {
        return revisions.selectCount(Wrappers.<IamRoleRevisionEntity>lambdaQuery()
                .eq(IamRoleRevisionEntity::getId, BigInteger.valueOf(revisionId))
                .eq(IamRoleRevisionEntity::getRoleId, BigInteger.valueOf(roleId))) > 0;
    }

    /**
     * 按锁定时的版本条件把授权引用改到新版本并将版本加一。
     *
     * @param assignmentId 授权 ID
     * @param revisionId 新角色版本 ID
     * @param currentVersion 锁定后的当前版本
     * @return 实际更新行数；并发改写时为 0
     */
    public int updateAssignmentRevision(long assignmentId, long revisionId, BigInteger currentVersion) {
        return assignments.update(Wrappers.<IamRoleAssignmentEntity>lambdaUpdate()
                .eq(IamRoleAssignmentEntity::getId, BigInteger.valueOf(assignmentId))
                .eq(IamRoleAssignmentEntity::getVersion, currentVersion)
                .set(IamRoleAssignmentEntity::getRevisionId, BigInteger.valueOf(revisionId))
                .set(IamRoleAssignmentEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 读取操作被角色引用的前提，含应用授权域与资源范围能力。
     *
     * @param actionIds 操作 ID；空集合返回空结果
     * @return 命中操作的能力投影
     */
    public List<AuthorizationEvalRows.Capability> listCapabilities(Collection<BigInteger> actionIds) {
        if (actionIds == null || actionIds.isEmpty()) {
            return List.of();
        }
        return actions.listCapabilities(actionIds);
    }

    private LambdaQueryWrapper<IamRoleDefinitionEntity> visible(AuthorizationDomain domain, boolean shared,
                                                                Long tenantId) {
        LambdaQueryWrapper<IamRoleDefinitionEntity> query = Wrappers.lambdaQuery();
        if (shared) {
            return query.eq(IamRoleDefinitionEntity::getKind, RoleKind.SHARED)
                    .isNull(IamRoleDefinitionEntity::getTenantId);
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return query.and(w -> w
                    .nested(n -> n.eq(IamRoleDefinitionEntity::getKind, RoleKind.SYSTEM)
                            .eq(IamRoleDefinitionEntity::getDomain, domain))
                    .or()
                    .eq(IamRoleDefinitionEntity::getKind, RoleKind.PLATFORM_CUSTOM));
        }
        return query.and(w -> w
                .nested(n -> n.eq(IamRoleDefinitionEntity::getKind, RoleKind.SYSTEM)
                        .eq(IamRoleDefinitionEntity::getDomain, domain)
                        .isNull(IamRoleDefinitionEntity::getTenantId))
                .or()
                .eq(IamRoleDefinitionEntity::getKind, RoleKind.SHARED)
                .or()
                .nested(n -> n.eq(IamRoleDefinitionEntity::getKind, RoleKind.TENANT_CUSTOM)
                        .eq(IamRoleDefinitionEntity::getTenantId, BigInteger.valueOf(tenantKey(tenantId)))));
    }

    private static long tenantKey(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
