package com.ingot.cloud.iam.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.TenantRoleDataRulePrivate;
import com.ingot.cloud.iam.mapper.TenantRoleDataRulePrivateMapper;
import com.ingot.cloud.iam.service.domain.TenantRoleDataRulePrivateService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>{@link TenantRoleDataRulePrivateService} 的默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TenantRoleDataRulePrivateServiceImpl
        extends BaseServiceImpl<TenantRoleDataRulePrivateMapper, TenantRoleDataRulePrivate>
        implements TenantRoleDataRulePrivateService {

    @Override
    @Cacheable(
            value = CacheConstants.TENANT_ROLE_DATA_RULES,
            key = "'role-' + #roleId + '-' + #platformRole",
            unless = "#result.isEmpty()"
    )
    public List<TenantRoleDataRulePrivate> listByRole(long roleId, boolean platformRole) {
        return CollUtil.emptyIfNull(list(Wrappers.<TenantRoleDataRulePrivate>lambdaQuery()
                .eq(TenantRoleDataRulePrivate::getRoleId, roleId)
                .eq(TenantRoleDataRulePrivate::getPlatformRole, platformRole)));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_DATA_RULES,
            key = "'role-' + #roleId + '-' + #platformRole"
    )
    @Transactional(rollbackFor = Exception.class)
    public void replace(long roleId, boolean platformRole, List<TenantRoleDataRulePrivate> rules) {
        remove(Wrappers.<TenantRoleDataRulePrivate>lambdaQuery()
                .eq(TenantRoleDataRulePrivate::getRoleId, roleId)
                .eq(TenantRoleDataRulePrivate::getPlatformRole, platformRole));
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        rules.forEach(rule -> {
            rule.setRoleId(roleId);
            rule.setPlatformRole(platformRole);
        });
        saveBatch(rules);
    }
}
