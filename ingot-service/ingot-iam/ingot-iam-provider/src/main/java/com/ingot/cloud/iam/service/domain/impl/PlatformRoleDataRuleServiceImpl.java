package com.ingot.cloud.iam.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.PlatformRoleDataRule;
import com.ingot.cloud.iam.mapper.PlatformRoleDataRuleMapper;
import com.ingot.cloud.iam.service.domain.PlatformRoleDataRuleService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>{@link PlatformRoleDataRuleService} 的默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class PlatformRoleDataRuleServiceImpl
        extends BaseServiceImpl<PlatformRoleDataRuleMapper, PlatformRoleDataRule>
        implements PlatformRoleDataRuleService {

    @Override
    @Cacheable(
            value = CacheConstants.PLATFORM_ROLE_DATA_RULES,
            key = "'role-' + #roleId",
            unless = "#result.isEmpty()"
    )
    public List<PlatformRoleDataRule> listByRoleId(long roleId) {
        return CollUtil.emptyIfNull(list(Wrappers.<PlatformRoleDataRule>lambdaQuery()
                .eq(PlatformRoleDataRule::getRoleId, roleId)));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_ROLE_DATA_RULES,
            key = "'role-' + #roleId"
    )
    @Transactional(rollbackFor = Exception.class)
    public void replace(long roleId, List<PlatformRoleDataRule> rules) {
        remove(Wrappers.<PlatformRoleDataRule>lambdaQuery()
                .eq(PlatformRoleDataRule::getRoleId, roleId));
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        rules.forEach(rule -> rule.setRoleId(roleId));
        saveBatch(rules);
    }
}
