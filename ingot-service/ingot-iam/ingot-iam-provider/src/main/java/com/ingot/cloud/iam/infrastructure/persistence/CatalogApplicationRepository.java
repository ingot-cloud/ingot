package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantAppEntitlementEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantAppEntitlementMapper;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.infrastructure.persistence.projection.ActionCatalogRow;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>维护目录应用及其启用事实，保留原有域和版本条件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogApplicationRepository {
    private final IamApplicationMapper applications;
    private final IamTenantAppEntitlementMapper entitlements;


    /**
     * 分页列出应用目录。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param domain 管理域，必填
     * @param name 应用名称包含匹配，空白表示不限制
     * @param enabled 启用状态，空表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用页
     */
    public Page<IamApplicationEntity> pageApplications(int page, int pageSize, AuthorizationDomain domain, String name,
                                                       Boolean enabled, Boolean baseline) {
        String keyword = IamFilters.containsName(name);
        return applications.selectPage(new Page<>(page, pageSize), Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getDomain, domain)
                .like(keyword != null, IamApplicationEntity::getName, keyword)
                .eq(enabled != null, IamApplicationEntity::getEnabled, enabled)
                .eq(baseline != null, IamApplicationEntity::getBaseline, baseline)
                .orderByAsc(IamApplicationEntity::getSortOrder)
                .orderByAsc(IamApplicationEntity::getId));
    }

    /**
     * 读取单个应用。
     *
     * @param id 应用 ID
     * @return 应用行，不存在时为空
     */
    public IamApplicationEntity findApplication(long id) {
        return applications.selectById(BigInteger.valueOf(id));
    }

    /**
     * 一次读取应用存在性及其有序操作目录。
     *
     * @param id 已通过入口访问检查的应用 ID
     * @param enabledOnly 是否只读取启用资源及操作
     * @return 目录行，应用不存在时为空列表
     */
    public List<ActionCatalogRow> actionCatalog(long id, boolean enabledOnly) {
        return applications.selectActionCatalog(BigInteger.valueOf(id), enabledOnly);
    }

    /**
     * 按应用 ID 批量读取目录名称与图标，供开通列表展示。
     *
     * @param applicationIds 应用 ID，可含空值与重复
     * @return 有名称或图标的目录项；找不到应用，或名称与图标都空白时不入表
     */
    public Map<BigInteger, ApplicationDisplay> applicationDisplays(Collection<BigInteger> applicationIds) {
        Map<BigInteger, ApplicationDisplay> displays = new LinkedHashMap<>();
        if (applicationIds == null || applicationIds.isEmpty()) {
            return displays;
        }
        List<BigInteger> ids = applicationIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return displays;
        }
        for (IamApplicationEntity row : applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .select(IamApplicationEntity::getId, IamApplicationEntity::getName, IamApplicationEntity::getIcon)
                .in(IamApplicationEntity::getId, ids))) {
            String name = blankToNull(row.getName());
            String icon = blankToNull(row.getIcon());
            if (name != null || icon != null) {
                displays.put(row.getId(), new ApplicationDisplay(name, icon));
            }
        }
        return displays;
    }

    /**
     * 按目录名称包含匹配应用 ID，供开通列表筛选。
     *
     * @param name 已规范化的名称关键字
     * @return 命中的应用 ID；关键字空白时为空
     */
    public List<BigInteger> applicationIdsByName(String name) {
        String keyword = IamFilters.containsName(name);
        if (keyword == null) {
            return List.of();
        }
        return applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .select(IamApplicationEntity::getId)
                .like(IamApplicationEntity::getName, keyword)).stream()
                .map(IamApplicationEntity::getId)
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 判断应用是否存在。
     *
     * @param id 应用 ID
     * @return 存在时为 true
     */
    public boolean existsApplication(long id) {
        return applications.selectCount(Wrappers.<IamApplicationEntity>lambdaQuery()
                .eq(IamApplicationEntity::getId, BigInteger.valueOf(id))) == 1;
    }

    /**
     * 插入应用目录项，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertApplication(IamApplicationEntity entity) {
        applications.insert(entity);
    }

    /**
     * 锁定应用行，调用方须处于事务中。
     *
     * @param id 应用 ID
     * @return 锁定行，不存在时为空
     */
    public IamApplicationEntity lockApplication(long id) {
        return applications.lockRow(BigInteger.valueOf(id));
    }

    /**
     * 更新应用展示信息并将版本加一。
     *
     * @param id 应用 ID
     * @param name 名称
     * @param description 说明，可空
     * @param icon 图标，可空
     * @param sortOrder 排序
     * @param baseline 是否基础开通
     * @param currentVersion 锁定后的当前版本
     */
    public void updateApplication(long id, String name, String description, String icon, int sortOrder,
                                  boolean baseline, BigInteger currentVersion) {
        applications.update(Wrappers.<IamApplicationEntity>lambdaUpdate()
                .eq(IamApplicationEntity::getId, BigInteger.valueOf(id))
                .set(IamApplicationEntity::getName, name)
                .set(IamApplicationEntity::getDescription, description)
                .set(IamApplicationEntity::getIcon, icon)
                .set(IamApplicationEntity::getSortOrder, sortOrder)
                .set(IamApplicationEntity::getBaseline, baseline)
                .set(IamApplicationEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 更新应用启用状态并将版本加一。
     *
     * @param id 应用 ID
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updateApplicationEnabled(long id, boolean enabled, BigInteger currentVersion) {
        applications.update(Wrappers.<IamApplicationEntity>lambdaUpdate()
                .eq(IamApplicationEntity::getId, BigInteger.valueOf(id))
                .set(IamApplicationEntity::getEnabled, enabled)
                .set(IamApplicationEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 删除应用。
     *
     * @param id 应用 ID
     */
    public void deleteApplication(long id) {
        applications.deleteById(BigInteger.valueOf(id));
    }

    /**
     * 统计引用该应用的开通数。
     *
     * @param applicationId 应用 ID
     * @return 开通条数
     */
    public long countEntitlements(long applicationId) {
        return entitlements.selectCount(Wrappers.<IamTenantAppEntitlementEntity>lambdaQuery()
                .eq(IamTenantAppEntitlementEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 按 ID 批量读取应用。
     *
     * @param ids 应用 ID
     * @return 应用行；空集合返回空列表
     */
    public List<IamApplicationEntity> listApplicationsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .in(IamApplicationEntity::getId, ids.stream().map(BigInteger::valueOf).toList()));
    }

    /**
     * 列出指定域启用的基础应用。
     *
     * @param domain 授权域
     * @return 基础应用 ID
     */
    public List<BigInteger> enabledBaselineIds(AuthorizationDomain domain) {
        return applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                        .eq(IamApplicationEntity::getDomain, domain)
                        .eq(IamApplicationEntity::getEnabled, true)
                        .eq(IamApplicationEntity::getBaseline, true)
                        .select(IamApplicationEntity::getId))
                .stream().map(IamApplicationEntity::getId).toList();
    }
}
