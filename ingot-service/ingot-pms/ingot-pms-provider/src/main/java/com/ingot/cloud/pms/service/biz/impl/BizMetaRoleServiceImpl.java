package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleItemVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.core.AuthorityUtils;
import com.ingot.cloud.pms.service.biz.BizMetaRoleService;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaRoleAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaRoleService;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.framework.commons.model.common.RelationDTO;
import com.ingot.framework.commons.model.support.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizMetaRoleServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 10:00.</p>
 */
@Service
@RequiredArgsConstructor
public class BizMetaRoleServiceImpl implements BizMetaRoleService {
    private final MetaRoleService roleService;
    private final MetaRoleAuthorityService roleAuthorityService;
    private final MetaAuthorityService authorityService;
    private final TenantRoleUserPrivateService roleUserPrivateService;

    private final RoleConvert roleConvert;
    private final AuthorityConvert authorityConvert;

    @Override
    public List<Option<Long>> options(MetaRole condition) {
        return roleService.list().stream()
                .filter(BizFilter.roleFilter(condition))
                .map(role -> Option.of(role.getId(), role.getName()))
                .toList();
    }

    @Override
    public List<RoleItemVO> conditionList(MetaRole condition) {
        return roleService.list().stream()
                .filter(BizFilter.roleFilter(condition))
                .map(role -> {
                    RoleItemVO item = roleConvert.to(role);
                    item.setTypeText(role.getType().getText());
                    item.setOrgTypeText(role.getOrgType().getText());
                    item.setScopeTypeText(role.getScopeType().getText());
                    item.setStatusText(role.getStatus().getText());
                    return item;
                }).toList();
    }

    @Override
    public List<AuthorityTreeNodeVO> getRoleAuthorities(long roleId) {
        List<Long> ids = roleAuthorityService.getRoleBindAuthorityIds(roleId);
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        List<MetaAuthority> authorities = authorityService.list(Wrappers.<MetaAuthority>lambdaQuery()
                .in(MetaAuthority::getId, ids));

        return AuthorityUtils.mapTree(authorities, authorityConvert, null);
    }

    @Override
    public void create(MetaRole params) {
        roleService.create(params);
    }

    @Override
    public void update(MetaRole params) {
        roleService.update(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        // 清除关联权限
        roleAuthorityService.clearByRoleId(id);
        // 清除关联用户
        roleUserPrivateService.clearByRoleId(id);
        // 删除角色
        roleService.delete(id);
    }

    @Override
    public void bindAuthorities(RelationDTO<Long, Long> params) {
        roleAuthorityService.roleBindAuthorities(params);
    }
}
