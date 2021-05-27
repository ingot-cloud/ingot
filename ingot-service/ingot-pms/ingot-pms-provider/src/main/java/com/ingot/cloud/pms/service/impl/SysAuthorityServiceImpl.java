package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.mapper.SysAuthorityMapper;
import com.ingot.cloud.pms.service.SysAuthorityService;
import com.ingot.cloud.pms.service.SysRoleAuthorityService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
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
    private final I18nService i18nService;
    private final IdGenerator idGenerator;

    @Override
    public void createAuthority(SysAuthority params) {
        // code 不能重复
        AssertionUtils.checkOperation(count(Wrappers.<SysAuthority>lambdaQuery()
                        .eq(SysAuthority::getCode, params.getCode())) == 0,
                i18nService.getMessage("SysAuthorityServiceImpl.ExistCode"));

        params.setId(idGenerator.nextId());
        params.setCreatedAt(DateUtils.now());
        AssertionUtils.checkOperation(save(params),
                i18nService.getMessage("SysAuthorityServiceImpl.CreateFailed"));
    }

    @Override
    public void updateAuthority(SysAuthority params) {
        if (StrUtil.isNotEmpty(params.getCode())) {
            // code 不能重复
            AssertionUtils.checkOperation(count(Wrappers.<SysAuthority>lambdaQuery()
                            .eq(SysAuthority::getCode, params.getCode())) == 0,
                    i18nService.getMessage("SysAuthorityServiceImpl.ExistCode"));
        }

        params.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(params),
                i18nService.getMessage("SysAuthorityServiceImpl.UpdateFailed"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAuthorityById(long id) {
        // 叶子权限才可以删除
        AssertionUtils.checkOperation(count(
                Wrappers.<SysAuthority>lambdaQuery().eq(SysAuthority::getPid, id)) == 0,
                i18nService.getMessage("SysAuthorityServiceImpl.RemoveFailedMustLeaf"));

        // 取消关联的角色
        sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                .eq(SysRoleAuthority::getAuthorityId, id));

        AssertionUtils.checkOperation(removeById(id),
                "SysAuthorityServiceImpl.RemoveFailed");
    }
}
