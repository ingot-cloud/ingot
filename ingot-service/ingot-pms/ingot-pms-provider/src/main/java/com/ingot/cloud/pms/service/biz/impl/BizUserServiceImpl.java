package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.vo.biz.ResetPwdVO;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.core.AuthorityUtils;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.biz.UserOpsChecker;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.core.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;
    private final SysMenuService sysMenuService;
    private final SysUserDeptService sysUserDeptService;
    private final SysRoleUserService sysRoleUserService;
    private final BizDeptService bizDeptService;
    private final BizRoleService bizRoleService;
    private final SysApplicationTenantService sysApplicationTenantService;

    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;
    private final UserOpsChecker userOpsChecker;
    private final UserConvert userConvert;

    @Override
    public UserProfileVO getUserProfile(long id) {
        SysUser user = sysUserService.getById(id);
        assertionChecker.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        UserProfileVO profile = userConvert.toUserProfile(user);
        List<SysUserTenant> userTenantList = sysUserTenantService.getUserOrgs(user.getId());
        profile.setOrgList(userTenantList);

        return profile;
    }

    @Override
    public void updateUserBaseInfo(long id, UserBaseInfoDTO params) {
        SysUser current = sysUserService.getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        SysUser user = userConvert.to(params);
        if (StrUtil.isNotEmpty(user.getPhone())
                && !StrUtil.equals(user.getPhone(), current.getPhone())) {
            assertionChecker.checkOperation(sysUserService.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, user.getPhone())) == 0,
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())
                && !StrUtil.equals(user.getEmail(), current.getEmail())) {
            assertionChecker.checkOperation(sysUserService.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, user.getEmail())) == 0,
                    "SysUserServiceImpl.EmailExist");
        }

        user.setUpdatedAt(DateUtils.now());
        assertionChecker.checkOperation(sysUserService.updateById(user),
                "SysUserServiceImpl.UpdateFailed");
    }

    @Override
    public List<MenuTreeNodeVO> getUserMenus(InUser user) {
        List<SysRole> roles = sysRoleService.getRolesOfUser(user.getId());
        List<SysAuthority> authorities = sysAuthorityService.getAuthorityAndChildrenByRoles(roles);

        List<SysAuthority> finallyAuthorities = AuthorityUtils.filterOrgLockAuthority(
                authorities, sysApplicationTenantService);
        return sysMenuService.getMenuByAuthorities(finallyAuthorities);
    }

    @Override
    public ResetPwdVO createUser(UserDTO params) {
        SysUser user = userConvert.to(params);

        // 默认初始化密码
        String initPwd = RandomUtil.randomString(6);

        user.setUsername(params.getPhone());
        user.setInitPwd(Boolean.TRUE);
        user.setPassword(initPwd);
        user.setStatus(UserStatusEnum.ENABLE);
        sysUserService.createUser(user);

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        result.setId(user.getId());
        return result;
    }

    @Override
    public void updateUser(UserDTO params) {
        if (params.getStatus() == UserStatusEnum.LOCK) {
            userOpsChecker.disableUser(params.getId());
        }

        SysUser user = userConvert.to(params);
        sysUserService.updateUser(user);
    }

    @Override
    public void deleteUser(long id) {
        userOpsChecker.removeUser(id);
        sysUserService.removeUserById(id);
    }

    @Override
    public ResetPwdVO resetPwd(long userId) {
        SysUser user = sysUserService.getById(userId);
        assertionChecker.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        // 重置密码
        String initPwd = RandomUtil.randomString(6);
        user.setPassword(passwordEncoder.encode(initPwd));
        user.updateById();

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        return result;
    }

    @Override
    public void userOrgEdit(UserOrgEditDTO params) {
        TenantEnv.runAs(params.getOrgId(), () -> {
            long userId = params.getId();
            SysTenant tenant = sysTenantService.getById(params.getOrgId());

            sysUserTenantService.joinTenant(userId, tenant);
            bizDeptService.setUserDeptsEnsureMainDept(userId, params.getDeptIds());
            bizRoleService.setOrgUserRoles(userId, params.getRoleIds());
        });
    }

    @Override
    public void userOrgLeave(UserOrgEditDTO params) {
        // 如果操作的是自己，那么不能离开当前组织
        long opsUserId = SecurityAuthContext.getUser().getId();
        if (opsUserId == params.getId()) {
            long currentOrg = TenantContextHolder.get();
            long leaveOrg = params.getOrgId();
            assertionChecker.checkOperation(currentOrg != leaveOrg, "BizUserServiceImpl.CantLeaveCurrentOrg");
        }

        TenantEnv.runAs(params.getOrgId(), () -> {
            long userId = params.getId();
            sysUserTenantService.leaveTenant(userId);
            // 取消关联部门
            sysUserDeptService.remove(Wrappers.<SysUserDept>lambdaQuery()
                    .eq(SysUserDept::getUserId, userId));
            // 取消关联角色
            sysRoleUserService.remove(Wrappers.<SysRoleUser>lambdaQuery()
                    .eq(SysRoleUser::getUserId, userId));
        });
    }

    @Override
    public List<UserOrgInfoVO> userOrgInfo(long userId) {
        List<SysUserTenant> list = CollUtil.emptyIfNull(sysUserTenantService.getUserOrgs(userId));
        return list.stream().map(org ->
                TenantEnv.applyAs(org.getTenantId(), () -> {
                    UserOrgInfoVO item = new UserOrgInfoVO();
                    item.setOrgId(org.getTenantId());

                    List<Long> deptIds = CollUtil.emptyIfNull(sysDeptService.getUserDepts(userId))
                            .stream().map(SysUserDept::getDeptId).toList();
                    item.setDeptIds(deptIds);

                    List<Long> roleIds = CollUtil.emptyIfNull(sysRoleService.getRolesOfUser(userId))
                            .stream().map(SysRole::getId).toList();
                    item.setRoleIds(roleIds);
                    return item;
                })).toList();
    }

    @Override
    public OrgUserProfileVO getOrgUserProfile(long id) {
        SysUser user = sysUserService.getById(id);
        assertionChecker.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        OrgUserProfileVO profile = userConvert.toOrgUserProfile(user);

        List<Long> deptIds = CollUtil.emptyIfNull(sysDeptService.getUserDepts(id))
                .stream().map(SysUserDept::getDeptId).toList();
        profile.setDeptIds(deptIds);
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgCreateUser(OrgUserDTO params) {
        // 手机号，部门，昵称不能为空
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPhone()), "SysUser.phone");
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getNickname()), "SysUser.nickname");
        assertionChecker.checkOperation(CollUtil.isNotEmpty(params.getDeptIds()), "SysUser.deptId");

        // 如果已经存在注册用户，那么直接关联新组织信息
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, params.getPhone()));
        if (user == null) {
            // 密码默认为手机号
            user = userConvert.to(params);
            user.setUsername(params.getPhone());
            user.setPassword(params.getPhone());
            user.setInitPwd(Boolean.TRUE);
            sysUserService.createUser(user);
        }

        // 加入租户
        SysTenant tenant = sysTenantService.getById(TenantContextHolder.get());
        sysUserTenantService.joinTenant(user.getId(), tenant);
        // 设置部门
        bizDeptService.setUserDeptsEnsureMainDept(user.getId(), params.getDeptIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgUpdateUser(OrgUserDTO params) {
        assertionChecker.checkOperation(params.getId() != null, "Common.IDNonNull");

        SysUser user = userConvert.to(params);
        // 更新用户
        sysUserService.updateUser(user);

        if (CollUtil.isNotEmpty(params.getDeptIds())) {
            // 更新部门
            bizDeptService.setUserDeptsEnsureMainDept(user.getId(), params.getDeptIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgDeleteUser(long id) {
        long userId = SecurityAuthContext.getUser().getId();
        assertionChecker.checkOperation(userId != id, "BizUserServiceImpl.RemoveSelfFailed");

        // 判断删除用户是否为除主管理员
        SysRole managerRole = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_ADMIN_CODE);
        long deleteCount = sysRoleUserService.count(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getRoleId, managerRole.getId())
                .eq(SysRoleUser::getUserId, id));
        assertionChecker.checkOperation(deleteCount == 0, "BizUserServiceImpl.CantRemoveManager");

        // 取消关联组织
        sysUserTenantService.leaveTenant(id);
        // 取消关联部门
        sysUserDeptService.remove(Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getUserId, id));
        // 取消关联角色
        sysRoleUserService.remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, id));
    }

    @Override
    public void orgPasswordInit(UserPasswordDTO params) {
        long id = SecurityAuthContext.getUser().getId();
        SysUser current = sysUserService.getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        if (!BooleanUtil.isTrue(current.getInitPwd())) {
            return;
        }

        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setInitPwd(false);
        assertionChecker.checkOperation(user.updateById(),
                "SysUserServiceImpl.UpdatePasswordFailed");
    }

    @Override
    public void fixPassword(UserPasswordDTO params) {
        sysUserService.fixPassword(SecurityAuthContext.getUser().getId(), params);
    }
}
