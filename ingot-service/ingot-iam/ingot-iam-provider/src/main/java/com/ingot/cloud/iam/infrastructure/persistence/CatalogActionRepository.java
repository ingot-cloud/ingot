package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>维护目录操作及依赖计数，不改变角色引用与启用检查。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogActionRepository {
    private final IamActionMapper actions;
    private final IamMenuActionMapper menuActions;
    private final IamRoleGrantMapper roleGrants;
    private final IamRoleDeltaMapper roleDeltas;


    /**
     * 统计资源下操作数。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作条数
     */
    public long countActions(long applicationId, long resourceId) {
        return actions.selectCount(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getResourceId, BigInteger.valueOf(resourceId)));
    }

    /**
     * 分页列出应用内操作。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param resourceId 所属资源，空表示不限制
     * @param name 操作名称包含匹配，空白表示不限制
     * @param ids 限定操作 ID，空表示不限制
     * @return 操作页
     */
    public Page<IamActionEntity> pageActions(long applicationId, int page, int pageSize, Long resourceId, String name,
                                             List<Long> ids) {
        String keyword = IamFilters.containsName(name);
        List<BigInteger> actionIds = ids == null ? List.of()
                : ids.stream().map(BigInteger::valueOf).toList();
        return actions.selectPage(new Page<>(page, pageSize), Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(resourceId != null, IamActionEntity::getResourceId,
                        resourceId == null ? null : BigInteger.valueOf(resourceId))
                .like(keyword != null, IamActionEntity::getName, keyword)
                .in(!actionIds.isEmpty(), IamActionEntity::getId, actionIds)
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 列出应用内全部操作，供操作目录组树。
     *
     * @param applicationId 应用 ID
     * @return 操作行
     */
    public List<IamActionEntity> listActions(long applicationId) {
        return actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 列出应用内全部启用操作，供角色权限选择一次组树。
     *
     * @param applicationId 应用 ID
     * @return 启用操作行
     */
    public List<IamActionEntity> listEnabledActions(long applicationId) {
        return actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getEnabled, true)
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 列出指定资源下的全部操作，供权限树展开。
     *
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 操作行
     */
    public List<IamActionEntity> listActionsByResource(long applicationId, long resourceId) {
        return actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getResourceId, BigInteger.valueOf(resourceId))
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 列出指定资源集合下的启用操作，供授权选择目录组树。
     *
     * @param applicationId 应用 ID
     * @param resourceIds 资源 ID
     * @return 启用操作行；空集合返回空列表
     */
    public List<IamActionEntity> listEnabledActionsByResourceIds(long applicationId, Collection<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        return actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .in(IamActionEntity::getResourceId, resourceIds.stream().map(BigInteger::valueOf).toList())
                .eq(IamActionEntity::getEnabled, true)
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 按操作 ID 批量读取，供回显解析。
     *
     * @param ids 操作 ID
     * @return 操作行；空集合返回空列表
     */
    public List<IamActionEntity> listActionsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                .in(IamActionEntity::getId, ids.stream().map(BigInteger::valueOf).toList())
                .orderByAsc(IamActionEntity::getId));
    }

    /**
     * 读取指定应用下的操作。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 操作行，不存在时为空
     */
    public IamActionEntity findAction(long applicationId, long id) {
        return actions.selectOne(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 判断应用下操作是否存在。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 存在时为 true
     */
    public boolean existsAction(long applicationId, long id) {
        return actions.selectCount(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id))) == 1;
    }

    /**
     * 插入操作，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertAction(IamActionEntity entity) {
        actions.insert(entity);
    }

    /**
     * 锁定操作行。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @return 锁定行，不存在时为空
     */
    public IamActionEntity lockAction(long applicationId, long id) {
        return actions.lockRow(BigInteger.valueOf(applicationId), BigInteger.valueOf(id));
    }

    /**
     * 更新操作名称并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @param name 名称
     * @param currentVersion 锁定后的当前版本
     */
    public void updateActionName(long applicationId, long id, String name, BigInteger currentVersion) {
        actions.update(Wrappers.<IamActionEntity>lambdaUpdate()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id))
                .set(IamActionEntity::getName, name)
                .set(IamActionEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 更新操作启用状态并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updateActionEnabled(long applicationId, long id, boolean enabled, BigInteger currentVersion) {
        actions.update(Wrappers.<IamActionEntity>lambdaUpdate()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id))
                .set(IamActionEntity::getEnabled, enabled)
                .set(IamActionEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除操作。
     *
     * @param applicationId 应用 ID
     * @param id 操作 ID
     */
    public void deleteAction(long applicationId, long id) {
        actions.delete(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamActionEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 统计菜单对该操作的引用。
     *
     * @param applicationId 应用 ID
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countMenuActions(long applicationId, long actionId) {
        return menuActions.selectCount(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .eq(IamMenuActionEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamMenuActionEntity::getActionId, BigInteger.valueOf(actionId)));
    }

    /**
     * 统计角色授权对该操作的引用。
     *
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countRoleGrants(long actionId) {
        return roleGrants.selectCount(Wrappers.<IamRoleGrantEntity>lambdaQuery()
                .eq(IamRoleGrantEntity::getActionId, BigInteger.valueOf(actionId)));
    }

    /**
     * 统计角色差异对该操作的引用。
     *
     * @param actionId 操作 ID
     * @return 引用条数
     */
    public long countRoleDeltas(long actionId) {
        return roleDeltas.selectCount(Wrappers.<IamRoleDeltaEntity>lambdaQuery()
                .eq(IamRoleDeltaEntity::getActionId, BigInteger.valueOf(actionId)));
    }
}
