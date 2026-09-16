package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ingot.cloud.iam.persistence.entity.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>读取创建组织所需的服务器目录引用，不信任客户端指定的授权版本。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class InitializationCatalogRepository {
    private final IamAccountMapper accounts;
    private final IamRoleDefinitionMapper roles;
    private final IamRoleRevisionMapper revisions;
    private final IamDefaultPolicyRevisionMapper defaults;
    private final IamApplicationMapper applications;
    private final IamPlanMapper plans;
    private final IamPlanApplicationMapper planApplications;

    /**
     * 检查组织所有者账号是否可用。
     * @param id 全局账号 ID
     * @return 是否存在启用且未删除的账号
     */
    public boolean activeAccount(long id) {
        return accounts.selectCount(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getId, BigInteger.valueOf(id)).eq(IamAccountEntity::getEnabled, true)
                .isNull(IamAccountEntity::getDeletedAt)) == 1;
    }

    /**
     * 读取指定域启用的系统治理角色，冷启动种子保证每域恰好一个。
     *
     * @param domain 管理域
     * @return 系统角色 ID，调用方检查唯一性
     */
    public List<Long> systemRoles(AuthorizationDomain domain) {
        return roles.selectList(Wrappers.<IamRoleDefinitionEntity>lambdaQuery().select(IamRoleDefinitionEntity::getId)
                .eq(IamRoleDefinitionEntity::getDomain, domain)
                .eq(IamRoleDefinitionEntity::getKind, RoleKind.SYSTEM)
                .isNull(IamRoleDefinitionEntity::getTenantId).eq(IamRoleDefinitionEntity::getEnabled, true))
                .stream().map(row -> row.getId().longValueExact()).toList();
    }

    /**
     * 按发布版本倒序读取系统角色版本。
     * @param roleId 系统角色 ID
     * @return 角色版本 ID 列表
     */
    public List<Long> systemRevisions(long roleId) {
        return revisions.selectList(Wrappers.<IamRoleRevisionEntity>lambdaQuery().select(IamRoleRevisionEntity::getId)
                .eq(IamRoleRevisionEntity::getRoleId, BigInteger.valueOf(roleId))
                .eq(IamRoleRevisionEntity::getKind, RoleKind.SYSTEM)
                .orderByDesc(IamRoleRevisionEntity::getRevision, IamRoleRevisionEntity::getId))
                .stream().map(row -> row.getId().longValueExact()).toList();
    }

    /**
     * 按发布版本倒序读取默认策略版本。
     * @param kind 默认策略类别
     * @return 策略版本 ID 列表
     */
    public List<Long> defaultRevisions(DefaultPolicyKind kind) {
        return defaults.selectList(Wrappers.<IamDefaultPolicyRevisionEntity>lambdaQuery()
                .select(IamDefaultPolicyRevisionEntity::getId).eq(IamDefaultPolicyRevisionEntity::getKind, kind)
                .orderByDesc(IamDefaultPolicyRevisionEntity::getRevision, IamDefaultPolicyRevisionEntity::getId))
                .stream().map(row -> row.getId().longValueExact()).toList();
    }

    /** @return 租户域启用的基础应用，按排序值及 ID 排序 */
    public List<ApplicationSummary> baselineApplications() {
        return applications.selectList(Wrappers.<IamApplicationEntity>lambdaQuery()
                .select(IamApplicationEntity::getId, IamApplicationEntity::getCode, IamApplicationEntity::getName,
                        IamApplicationEntity::getIcon, IamApplicationEntity::getSortOrder)
                .eq(IamApplicationEntity::getDomain, AuthorizationDomain.TENANT)
                .eq(IamApplicationEntity::getEnabled, true).eq(IamApplicationEntity::getBaseline, true)
                .orderByAsc(IamApplicationEntity::getSortOrder, IamApplicationEntity::getId))
                .stream().map(this::summary).toList();
    }

    /**
     * 检查套餐可用性。
     * @param planId 套餐 ID
     * @return 套餐是否启用
     */
    public boolean activePlan(long planId) {
        return plans.selectCount(Wrappers.<IamPlanEntity>lambdaQuery().eq(IamPlanEntity::getId, BigInteger.valueOf(planId))
                .eq(IamPlanEntity::getEnabled, true)) == 1;
    }

    /**
     * 读取套餐内启用的租户应用。
     * @param planId 套餐 ID
     * @return 应用列表
     */
    public List<ApplicationSummary> planApplications(long planId) {
        var query = new MPJLambdaWrapper<IamPlanApplicationEntity>()
                .select(IamApplicationEntity::getId, IamApplicationEntity::getCode, IamApplicationEntity::getName,
                        IamApplicationEntity::getIcon, IamApplicationEntity::getSortOrder)
                .innerJoin(IamApplicationEntity.class, IamApplicationEntity::getId, IamPlanApplicationEntity::getApplicationId)
                .eq(IamPlanApplicationEntity::getPlanId, BigInteger.valueOf(planId))
                .eq(IamApplicationEntity::getDomain, AuthorizationDomain.TENANT)
                .eq(IamApplicationEntity::getEnabled, true)
                .orderByAsc(IamApplicationEntity::getSortOrder, IamApplicationEntity::getId);
        return planApplications.selectJoinList(IamApplicationEntity.class, query).stream().map(this::summary).toList();
    }

    /**
     * 统计套餐的全部应用关联，用于识别停用或跨域配置。
     * @param planId 套餐 ID
     * @return 关联数量
     */
    public long planApplicationCount(long planId) {
        return planApplications.selectCount(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                .eq(IamPlanApplicationEntity::getPlanId, BigInteger.valueOf(planId)));
    }

    private ApplicationSummary summary(IamApplicationEntity app) {
        return new ApplicationSummary(app.getId().toString(), app.getCode(), app.getName(), app.getIcon(), app.getSortOrder());
    }
}
