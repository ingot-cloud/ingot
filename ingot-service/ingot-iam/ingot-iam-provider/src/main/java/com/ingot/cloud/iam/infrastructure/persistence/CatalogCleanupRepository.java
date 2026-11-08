package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAppAudienceEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAudienceDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAudienceGroupEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAudienceMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlanApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleGrantEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantAppEntitlementEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAppAudienceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAudienceDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAudienceGroupMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamAudienceMemberMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuActionMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMenuMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamPlanApplicationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamResourceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleDeltaMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamRoleGrantMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantAppEntitlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


/**
 * <p>按既有外键依赖顺序清除应用及其关联，事务由调用用例负责。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CatalogCleanupRepository {
    private final IamApplicationMapper applications;
    private final IamResourceMapper resources;
    private final IamActionMapper actions;
    private final IamMenuMapper menus;
    private final IamMenuActionMapper menuActions;
    private final IamPlanApplicationMapper planApplications;
    private final IamTenantAppEntitlementMapper entitlements;
    private final IamRoleGrantMapper roleGrants;
    private final IamRoleDeltaMapper roleDeltas;
    private final IamAppAudienceMapper appAudiences;
    private final IamAudienceMemberMapper audienceMembers;
    private final IamAudienceGroupMapper audienceGroups;
    private final IamAudienceDepartmentMapper audienceDepartments;


    /**
     * 按依赖顺序清除该应用全部关联后删除应用行。
     *
     * @param applicationId 应用 ID
     */
    public void purgeApplication(long applicationId) {
        BigInteger application = BigInteger.valueOf(applicationId);
        List<BigInteger> actionIds = actions.selectList(Wrappers.<IamActionEntity>lambdaQuery()
                        .select(IamActionEntity::getId)
                        .eq(IamActionEntity::getApplicationId, application))
                .stream().map(IamActionEntity::getId).toList();
        if (!actionIds.isEmpty()) {
            roleGrants.delete(Wrappers.<IamRoleGrantEntity>lambdaQuery()
                    .in(IamRoleGrantEntity::getActionId, actionIds));
            roleDeltas.delete(Wrappers.<IamRoleDeltaEntity>lambdaQuery()
                    .in(IamRoleDeltaEntity::getActionId, actionIds));
        }
        menuActions.delete(Wrappers.<IamMenuActionEntity>lambdaQuery()
                .eq(IamMenuActionEntity::getApplicationId, application));
        menus.delete(Wrappers.<IamMenuEntity>lambdaQuery()
                .eq(IamMenuEntity::getApplicationId, application));
        audienceMembers.delete(Wrappers.<IamAudienceMemberEntity>lambdaQuery()
                .eq(IamAudienceMemberEntity::getApplicationId, application));
        audienceGroups.delete(Wrappers.<IamAudienceGroupEntity>lambdaQuery()
                .eq(IamAudienceGroupEntity::getApplicationId, application));
        audienceDepartments.delete(Wrappers.<IamAudienceDepartmentEntity>lambdaQuery()
                .eq(IamAudienceDepartmentEntity::getApplicationId, application));
        appAudiences.delete(Wrappers.<IamAppAudienceEntity>lambdaQuery()
                .eq(IamAppAudienceEntity::getApplicationId, application));
        entitlements.delete(Wrappers.<IamTenantAppEntitlementEntity>lambdaQuery()
                .eq(IamTenantAppEntitlementEntity::getApplicationId, application));
        planApplications.delete(Wrappers.<IamPlanApplicationEntity>lambdaQuery()
                .eq(IamPlanApplicationEntity::getApplicationId, application));
        actions.delete(Wrappers.<IamActionEntity>lambdaQuery()
                .eq(IamActionEntity::getApplicationId, application));
        resources.delete(Wrappers.<IamResourceEntity>lambdaQuery()
                .eq(IamResourceEntity::getApplicationId, application));
        applications.deleteById(application);
    }
}
