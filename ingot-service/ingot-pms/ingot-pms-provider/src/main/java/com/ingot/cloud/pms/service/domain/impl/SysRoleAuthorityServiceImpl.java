package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleAuthorityMapper;
import com.ingot.cloud.pms.service.domain.SysRoleAuthorityService;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SysRoleAuthorityServiceImpl extends CommonRoleRelationService<SysRoleAuthorityMapper, SysRoleAuthority> implements SysRoleAuthorityService {
    private final AuthorityTrans authorityTrans;

    private final Do remove = (roleId, targetId) -> remove(Wrappers.<SysRoleAuthority>lambdaQuery()
            .eq(SysRoleAuthority::getRoleId, roleId)
            .eq(SysRoleAuthority::getAuthorityId, targetId));
    private final Do bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authorityBindRoles(RelationDTO<Integer, Integer> params) {
        bindRoles(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindAuthorities(RelationDTO<Integer, Integer> params) {
        bindTargets(params, remove, bind,
                "SysRoleAuthorityServiceImpl.RemoveFailed");
    }

    @Override
    public List<AuthorityTreeNodeVO> getRoleAuthorities(int roleId,
                                                        boolean isBind,
                                                        SysAuthority condition) {
        List<SysAuthority> all = baseMapper.getRoleAuthorities(roleId, isBind, condition);
        List<AuthorityTreeNodeVO> allNode = all.stream()
                .map(authorityTrans::to).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtils.build(allNode, 0);

        if (isBind) {
            allNode.forEach(item -> {
                if (!TreeUtils.contains(tree, item)) {
                    tree.add(item);
                }
            });
        }

        return tree;
    }
}
