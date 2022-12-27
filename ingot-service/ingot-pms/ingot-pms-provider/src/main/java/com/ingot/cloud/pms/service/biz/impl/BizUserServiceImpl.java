package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysMenuService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.userdetails.IngotUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : BizUserServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 6:46 PM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizUserServiceImpl implements BizUserService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysMenuService sysMenuService;
    private final AssertionChecker assertI18nService;
    private final UserTrans userTrans;

    @Override
    public UserProfileVO getUserProfile(long id) {
        SysUser user = sysUserService.getById(id);
        assertI18nService.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        UserProfileVO profile = userTrans.toUserProfile(user);

        List<SysRole> list = sysRoleService.getRolesOfUser(id);
        if (CollUtil.isNotEmpty(list)) {
            profile.setRoleIds(list.stream()
                    .map(SysRole::getId).collect(Collectors.toList()));
        }

        return profile;
    }

    @Override
    public void updateUserBaseInfo(long id, UserBaseInfoDTO params) {
        SysUser current = sysUserService.getById(id);
        assertI18nService.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        SysUser user = userTrans.to(params);
        if (StrUtil.isNotEmpty(user.getPhone())
                && !StrUtil.equals(user.getPhone(), current.getPhone())) {
            assertI18nService.checkOperation(sysUserService.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, user.getPhone())) == 0,
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())
                && !StrUtil.equals(user.getEmail(), current.getEmail())) {
            assertI18nService.checkOperation(sysUserService.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, user.getEmail())) == 0,
                    "SysUserServiceImpl.EmailExist");
        }

        user.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(sysUserService.updateById(user),
                "SysUserServiceImpl.UpdateFailed");
    }

    @Override
    public List<MenuTreeNodeVO> getUserMenus(IngotUser user) {
        List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
        List<SysAuthority> authorities = sysAuthorityService.getAuthorityAndChildrenByRoles(roles);
        return sysMenuService.getMenuByAuthorities(authorities);
    }
}
