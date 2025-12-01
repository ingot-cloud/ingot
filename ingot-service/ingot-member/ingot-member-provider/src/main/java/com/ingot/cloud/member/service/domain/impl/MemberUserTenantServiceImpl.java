package com.ingot.cloud.member.service.domain.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.cloud.member.mapper.MemberUserTenantMapper;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Service
public class MemberUserTenantServiceImpl extends BaseServiceImpl<MemberUserTenantMapper, MemberUserTenant> implements MemberUserTenantService {

    @Override
    public void joinTenant(long userId, long tenantId) {
//        List<MemberUserTenant> joinedOrgList = CollUtil.emptyIfNull(list(Wrappers.<MemberUserTenant>lambdaQuery()
//                .eq(MemberUserTenant::getUserId, userId)));
//
//        long joinId = tenant.getId();
//
//        // 如果已经加入过，那么不处理
//        if (joinedOrgList.stream().anyMatch(item -> Objects.equals(item.getTenantId(), joinId))) {
//            return;
//        }
//
//        MemberUserTenant userTenant = new MemberUserTenant();
//        userTenant.setUserId(userId);
//        userTenant.setTenantId(joinId);
//        userTenant.setMain(CollUtil.isEmpty(joinedOrgList));
//        userTenant.setName(tenant.getName());
//        userTenant.setAvatar(tenant.getAvatar());
//        userTenant.setCreatedAt(DateUtil.now());
//        save(userTenant);
    }

    @Override
    public void leaveTenant(long userId) {
        remove(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getUserId, userId)
                .eq(MemberUserTenant::getTenantId, TenantContextHolder.get()));
    }

    @Override
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getTenantId, tenantId));
    }

    @Override
    public void updateBase(long tenantId) {
//        boolean needUpdate = false;
//        MemberUserTenant entity = new MemberUserTenant();
//        if (StrUtil.isNotEmpty(params.getName())) {
//            entity.setName(params.getName());
//            needUpdate = true;
//        }
//        if (StrUtil.isNotEmpty(params.getAvatar())) {
//            entity.setAvatar(params.getAvatar());
//            needUpdate = true;
//        }
//
//        if (!needUpdate) {
//            return;
//        }
//
//        update(entity, Wrappers.<MemberUserTenant>lambdaUpdate()
//                .eq(MemberUserTenant::getTenantId, params.getId()));
    }

    @Override
    public List<MemberUserTenant> getUserOrgs(long userId) {
        return list(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getUserId, userId));
    }
}
