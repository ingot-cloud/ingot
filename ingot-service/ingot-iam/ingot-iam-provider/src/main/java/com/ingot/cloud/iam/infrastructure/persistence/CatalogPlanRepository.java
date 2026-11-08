package com.ingot.cloud.iam.infrastructure.persistence;

import java.util.stream.Collectors;
import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlanApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlanEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlanApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlanMapper;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>维护套餐及应用绑定，保留原有差量保存与版本检查。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogPlanRepository {
    private final IamPlanMapper plans;
    private final IamPlanApplicationMapper planApplications;


    /**
     * 统计套餐对该应用的引用。
     *
     * @param applicationId 应用 ID
     * @return 引用条数
     */
    public long countPlanApplications(long applicationId) {
        return planApplications.selectCount(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                .eq(IamPlanApplicationEntity::getApplicationId, BigInteger.valueOf(applicationId)));
    }

    /**
     * 分页列出套餐。
     *
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，空白表示不限制
     * @param enabled 启用状态，空表示不限制
     * @return 套餐页
     */
    public Page<IamPlanEntity> pagePlans(int page, int pageSize, String name, Boolean enabled) {
        String keyword = IamFilters.containsName(name);
        return plans.selectPage(new Page<>(page, pageSize), Wrappers.<IamPlanEntity>lambdaQuery()
                .like(keyword != null, IamPlanEntity::getName, keyword)
                .eq(enabled != null, IamPlanEntity::getEnabled, enabled)
                .orderByAsc(IamPlanEntity::getId));
    }

    /**
     * 读取套餐。
     *
     * @param id 套餐 ID
     * @return 套餐行，不存在时为空
     */
    public IamPlanEntity findPlan(long id) {
        return plans.selectById(BigInteger.valueOf(id));
    }

    /**
     * 插入套餐，版本由数据库默认值承担。
     *
     * @param entity 待插入行
     */
    public void insertPlan(IamPlanEntity entity) {
        plans.insert(entity);
    }

    /**
     * 锁定套餐行。
     *
     * @param id 套餐 ID
     * @return 锁定行，不存在时为空
     */
    public IamPlanEntity lockPlan(long id) {
        return plans.lockRow(BigInteger.valueOf(id));
    }

    /**
     * 更新套餐名称、说明与启停状态，并将版本加一。
     *
     * @param id 套餐 ID
     * @param name 名称
     * @param description 说明，可空
     * @param enabled 是否启用
     * @param currentVersion 锁定后的当前版本
     */
    public void updatePlan(long id, String name, String description, boolean enabled, BigInteger currentVersion) {
        plans.update(Wrappers.<IamPlanEntity>lambdaUpdate()
                .eq(IamPlanEntity::getId, BigInteger.valueOf(id))
                .set(IamPlanEntity::getName, name)
                .set(IamPlanEntity::getDescription, description)
                .set(IamPlanEntity::getEnabled, enabled)
                .set(IamPlanEntity::getVersion, currentVersion.add(BigInteger.ONE)));
    }

    /**
     * 整体替换套餐应用清单。
     *
     * @param planId 套餐 ID
     * @param applicationIds 应用 ID
     */
    public void replacePlanApplications(long planId, List<Long> applicationIds) {
        var after = CatalogRelationIds.uniqueRelationIds(applicationIds);
        var before = planApplicationIds(planId);
        BigInteger plan = BigInteger.valueOf(planId);
        before.stream().filter(id -> !after.contains(id.longValueExact())).forEach(id ->
                planApplications.delete(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                        .eq(IamPlanApplicationEntity::getPlanId, plan)
                        .eq(IamPlanApplicationEntity::getApplicationId, id)));
        var oldIds = before.stream().map(BigInteger::longValueExact).collect(Collectors.toSet());
        after.stream().filter(id -> !oldIds.contains(id)).forEach(id -> {
            var row = new IamPlanApplicationEntity();
            row.setPlanId(plan);
            row.setApplicationId(BigInteger.valueOf(id));
            planApplications.insert(row);
        });
    }

    /**
     * 读取套餐绑定的应用 ID，按应用 ID 排序。
     *
     * @param planId 套餐 ID
     * @return 应用 ID
     */
    public List<BigInteger> planApplicationIds(long planId) {
        return planApplications.selectList(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                        .eq(IamPlanApplicationEntity::getPlanId, BigInteger.valueOf(planId))
                        .orderByAsc(IamPlanApplicationEntity::getApplicationId))
                .stream().map(IamPlanApplicationEntity::getApplicationId).toList();
    }
}
