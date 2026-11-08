package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>维护目录资源及其能力定义，查询限定真实应用归属。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogResourceRepository {
    private final IamResourceMapper resources;


    /**
     * 统计应用下资源数。
     *
     * @param applicationId 应用 ID
     * @return 资源条数
     */
    public long countResources(long applicationId) {
        return resources.selectCount(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 分页列出应用内资源。
     *
     * @param applicationId 应用 ID
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param name 资源名称包含匹配，空白表示不限制
     * @param code 资源编码包含匹配，空白表示不限制
     * @return 资源页
     */
    public Page<IamResourceEntity> pageResources(long applicationId, int page, int pageSize, String name, String code) {
        String nameKeyword = IamFilters.containsName(name);
        String codeKeyword = IamFilters.containsName(code);
        return resources.selectPage(new Page<>(page, pageSize), Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .like(nameKeyword != null, IamResourceEntity::getName, nameKeyword)
                .like(codeKeyword != null, IamResourceEntity::getCode, codeKeyword)
                .orderByAsc(IamResourceEntity::getId));
    }

    /**
     * 列出应用内全部启用资源，供角色权限选择一次组树。
     *
     * @param applicationId 应用 ID
     * @return 启用资源行
     */
    public List<IamResourceEntity> listEnabledResources(long applicationId) {
        return resources.selectList(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getEnabled, true)
                .orderByAsc(IamResourceEntity::getId));
    }

    /**
     * 列出应用内全部资源，供操作目录组树。
     *
     * @param applicationId 应用 ID
     * @return 资源行
     */
    public List<IamResourceEntity> listResources(long applicationId) {
        return resources.selectList(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .orderByAsc(IamResourceEntity::getId));
    }

    /**
     * 按 ID 批量读取资源。
     *
     * @param ids 资源 ID
     * @return 资源行；空集合返回空列表
     */
    public List<IamResourceEntity> listResourcesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return resources.selectList(Wrappers.<IamResourceEntity>lambdaQuery()
                .in(IamResourceEntity::getId, ids.stream().map(BigInteger::valueOf).toList()));
    }

    /**
     * 读取指定应用下的资源。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @return 资源行，不存在时为空
     */
    public IamResourceEntity findResource(long applicationId, long id) {
        return resources.selectOne(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getId, BigInteger.valueOf(id)));
    }

    /**
     * 插入资源，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertResource(IamResourceEntity entity) {
        resources.insert(entity);
    }

    /**
     * 锁定资源行。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @return 锁定行，不存在时为空
     */
    public IamResourceEntity lockResource(long applicationId, long id) {
        return resources.lockRow(BigInteger.valueOf(applicationId), BigInteger.valueOf(id));
    }

    /**
     * 更新资源名称与能力并将版本加一。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     * @param name 名称
     * @param scopes JSON 数组
     * @param fields JSON 数组
     * @param fieldPolicy 是否接入租户字段策略
     * @param currentVersion 锁定后的当前版本
     */
    public void updateResource(long applicationId, long id, String name, String scopes, String fields,
                               boolean fieldPolicy, BigInteger currentVersion) {
        resources.update(Wrappers.<IamResourceEntity>lambdaUpdate()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getId, BigInteger.valueOf(id))
                .set(IamResourceEntity::getName, name)
                .set(IamResourceEntity::getScopeCapabilities, scopes)
                .set(IamResourceEntity::getFieldCapabilities, fields)
                .set(IamResourceEntity::getFieldPolicy, fieldPolicy)
                .set(IamResourceEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除资源。
     *
     * @param applicationId 应用 ID
     * @param id 资源 ID
     */
    public void deleteResource(long applicationId, long id) {
        resources.delete(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, BigInteger.valueOf(applicationId))
                .eq(IamResourceEntity::getId, BigInteger.valueOf(id)));
    }
}
