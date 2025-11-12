package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.enums.AuthorityTypeEnum;
import com.ingot.cloud.pms.service.biz.BizMetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaAuthorityService;
import com.ingot.cloud.pms.service.domain.MetaRoleAuthorityService;
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

    private final AssertionChecker assertionChecker;

    @Override
    public void createNonMenuAuthority(MetaAuthority authority) {
        assertionChecker.checkOperation(authority.getType() != AuthorityTypeEnum.MENU,
                "BizMetaAuthorityServiceImpl.CantCreateMenuAuthority");

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

        // 清空角色权限关联
        roleAuthorityService.clearByAuthorityId(id);
        // 删除权限
        authorityService.delete(id);
    }
}
