package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.IngotUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    private final SysDeptService sysDeptService;
    private final SysUserTenantService sysUserTenantService;
    private final SysMenuService sysMenuService;
    private final SysUserDeptService sysUserDeptService;
    private final SysRoleUserService sysRoleUserService;
    private final BizDeptService bizDeptService;

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

        List<Long> deptIds = CollUtil.emptyIfNull(sysDeptService.getUserDepts(id))
                .stream().map(SysUserDept::getDeptId).toList();
        profile.setDeptIds(deptIds);

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
        List<SysRole> roles = sysRoleService.getRolesOfUser(user.getId());
        List<SysAuthority> authorities = sysAuthorityService.getAuthorityAndChildrenByRoles(roles);
        return sysMenuService.getMenuByAuthorities(authorities);
    }

    @Override
    public UserProfileVO getOrgUserProfile(long id) {
        SysUser user = sysUserService.getById(id);
        assertI18nService.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        UserProfileVO profile = userTrans.toUserProfile(user);

        List<Long> deptIds = CollUtil.emptyIfNull(sysDeptService.getUserDepts(id))
                .stream().map(SysUserDept::getDeptId).toList();
        profile.setDeptIds(deptIds);
        return profile;
    }

    @Override
    public void orgCreateUser(UserDTO params) {
        params.setUsername(params.getPhone());
        // 如果已经存在注册用户，那么直接关联新组织信息
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, params.getPhone()));
        if (user == null) {
            // 密码默认为手机号
            params.setInitPwd(Boolean.TRUE);
            user = userTrans.to(params);
            user.setPassword(params.getPhone());
            sysUserService.createUser(user);
        }

        // 加入租户
        sysUserTenantService.joinTenant(user.getId());
        // 设置部门
        bizDeptService.setUserDeptsEnsureMainDept(user.getId(), params.getDeptIds());
    }

    @Override
    public void orgUpdateUser(UserDTO params) {
        params.setNewPassword(null);
        params.setPassword(null);
        params.setRoleIds(null);
        SysUser user = userTrans.to(params);
        // 更新用户
        sysUserService.updateUser(user);

        if (CollUtil.isNotEmpty(params.getDeptIds())) {
            // 更新部门
            bizDeptService.setUserDeptsEnsureMainDept(user.getId(), params.getDeptIds());
        }
    }

    @Override
    public void orgDeleteUser(long id) {
        long userId = SecurityAuthContext.getUser().getId();
        assertI18nService.checkOperation(userId != id, "BizUserServiceImpl.RemoveSelfFailed");

        // 判断删除用户是否为除主管理员
        SysRole managerRole = sysRoleService.getRoleByCode(RoleConstants.ROLE_MANAGER_CODE);
        long deleteCount = sysRoleUserService.count(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getRoleId, managerRole.getId())
                .eq(SysRoleUser::getUserId, id));
        assertI18nService.checkOperation(deleteCount == 0, "BizUserServiceImpl.CantRemoveManager");

        // 取消关联组织
        sysUserTenantService.leaveTenant(id);
        // 取消关联部门
        sysUserDeptService.remove(Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getUserId, id));
        // 取消关联角色
        sysRoleUserService.remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, id));
    }
}
