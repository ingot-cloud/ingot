package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.mapper.SysAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysAuthorityServiceImpl extends BaseServiceImpl<SysAuthorityMapper, SysAuthority> implements SysAuthorityService {
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final AssertI18nService assertI18nService;
    private final IdGenerator idGenerator;

    @Override
    public void createAuthority(SysAuthority params) {
        // code 不能重复
        assertI18nService.checkOperation(count(Wrappers.<SysAuthority>lambdaQuery()
                        .eq(SysAuthority::getCode, params.getCode())) == 0,
                "SysAuthorityServiceImpl.ExistCode");

        params.setId(idGenerator.nextId());
        params.setCreatedAt(DateUtils.now());
        assertI18nService.checkOperation(save(params),
                "SysAuthorityServiceImpl.CreateFailed");
    }

    @Override
    public void updateAuthority(SysAuthority params) {
        if (StrUtil.isNotEmpty(params.getCode())) {
            // code 不能重复
            assertI18nService.checkOperation(count(Wrappers.<SysAuthority>lambdaQuery()
                            .eq(SysAuthority::getCode, params.getCode())) == 0,
                    "SysAuthorityServiceImpl.ExistCode");
        }

        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysAuthorityServiceImpl.UpdateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAuthorityById(long id) {
        // 叶子权限才可以删除
        boolean result = count(Wrappers.<SysAuthority>lambdaQuery().eq(SysAuthority::getPid, id)) == 0;
        assertI18nService.checkOperation(result, "SysAuthorityServiceImpl.RemoveFailedMustLeaf");

        // 取消关联的角色
        result = sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(SysRoleAuthority::getAuthorityId, id));
        assertI18nService.checkOperation(result, "SysAuthorityServiceImpl.RemoveFailed");

        result = removeById(id);
        assertI18nService.checkOperation(result, "SysAuthorityServiceImpl.RemoveFailed");
    }
}
