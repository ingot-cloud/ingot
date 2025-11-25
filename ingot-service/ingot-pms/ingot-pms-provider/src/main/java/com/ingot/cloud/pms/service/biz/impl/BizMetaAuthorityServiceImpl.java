package com.ingot.cloud.pms.service.biz.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.enums.AuthorityTypeEnum;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.service.biz.BizMetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaAppService;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaRoleAuthorityService;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizMetaAuthorityServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 15:00.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizMetaAuthorityServiceImpl implements BizMetaAuthorityService {
    private final MetaAuthorityService authorityService;
    private final MetaRoleAuthorityService roleAuthorityService;
    private final MetaAppService appService;

    private final AuthorityConvert authorityConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public List<AuthorityTreeNodeVO> treeList(MetaAuthority filter) {
        List<AuthorityTreeNodeVO> nodeList = authorityService.list()
                .stream()
                .filter(BizFilter.authorityFilter(filter))
                .sorted(Comparator.comparing(AuthorityType::getOrgType)
                        .thenComparing(AuthorityType::getId))
                .map(authorityConvert::toTreeNode).collect(Collectors.toList());

        List<AuthorityTreeNodeVO> tree = TreeUtil.build(nodeList);
        TreeUtil.compensate(tree, nodeList);
        return tree;
    }

    @Override
    public void createNonMenuAuthority(MetaAuthority authority) {
        assertionChecker.checkOperation(authority.getType() != AuthorityTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantCreateMenuAuthority");

        if (authority.getPid() != null) {
            MetaAuthority parent = authorityService.getById(authority.getPid());
            assertionChecker.checkOperation(parent != null, "BizMetaAuthorityServiceImpl.ParentNotExist");
            assert parent != null;
            authority.setType(parent.getType());
            authority.setOrgType(parent.getOrgType());
        }

        authorityService.create(authority, true);
    }

    @Override
    public void updateNonMenuAuthority(MetaAuthority authority) {
        MetaAuthority current = authorityService.getById(authority.getId());
        assertionChecker.checkOperation(current != null, "BizMetaAuthorityServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != AuthorityTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantUpdateMenuAuthority");

        authorityService.update(authority);
    }

    @Override
    public void deleteNonMenuAuthority(long id) {
        MetaAuthority current = authorityService.getById(id);
        assertionChecker.checkOperation(current != null, "BizMetaAuthorityServiceImpl.NotExist");
        assert current != null;
        assertionChecker.checkOperation(current.getType() != AuthorityTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantDeleteMenuAuthority");

        // 判断是否为应用，如果是应用那么不可删除
        assertionChecker.checkOperation(appService.count(Wrappers.<MetaApp>lambdaQuery()
                        .eq(MetaApp::getMenuId, id)) == 0,
                "BizMetaAuthorityServiceImpl.IsApplication");

        // 清空角色权限关联
        roleAuthorityService.clearByAuthorityId(id);
        // 删除权限
        authorityService.delete(id);
    }
}
