package com.ingot.cloud.pms.service.biz.impl;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.bo.role.BizAssignRoleBO;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.biz.ResetPwdVO;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.core.BizRoleUtils;
import com.ingot.cloud.pms.service.biz.*;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;
    private final SysUserSocialService sysUserSocialService;

    private final MetaRoleService metaRoleService;
    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;
    private final TenantUserDeptPrivateService tenantUserDeptPrivateService;
    private final BizRoleService bizRoleService;
    private final BizDeptService bizDeptService;

    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;
    private final UserOpsChecker userOpsChecker;
    private final UserConvert userConvert;

    @Override
    public List<Long> getUserDeptIds(long userId) {
        return CollUtil.emptyIfNull(tenantUserDeptPrivateService.getUserDepartmentIds(userId));
    }

    @Override
    public List<TenantDept> getUserDescendant(long userId, boolean includeSelf) {
        List<Long> deptIds = getUserDeptIds(userId);
        if (CollUtil.isEmpty(deptIds)) {
            return ListUtil.empty();
        }

        return deptIds.stream()
                .flatMap(deptId -> bizDeptService.getDescendantList(deptId, includeSelf).stream())
                .toList();
    }

    @Override
    public List<RoleType> getUserRoles(long userId) {
        List<TenantRoleUserPrivate> roleUserPrivateList = tenantRoleUserPrivateService.getUserRoles(userId);
        if (CollUtil.isEmpty(roleUserPrivateList)) {
            return ListUtil.empty();
        }

        List<RoleType> result = new ArrayList<>(roleUserPrivateList.size());

        List<Long> metaRoleIds = roleUserPrivateList.stream()
                .filter(item -> BooleanUtil.isTrue(item.getMetaRole()))
                .map(TenantRoleUserPrivate::getRoleId)
                .toList();
        if (CollUtil.isNotEmpty(metaRoleIds)) {
            List<MetaRole> metaRoleList = metaRoleService.list(Wrappers.<MetaRole>lambdaQuery()
                    .eq(MetaRole::getStatus, CommonStatusEnum.ENABLE)
                    .in(MetaRole::getId, metaRoleIds));
            result.addAll(metaRoleList);
        }

        List<Long> privateRoleIds = roleUserPrivateList.stream()
                .filter(item -> BooleanUtil.isFalse(item.getMetaRole()))
                .map(TenantRoleUserPrivate::getRoleId)
                .toList();
        if (CollUtil.isNotEmpty(privateRoleIds)) {
            List<TenantRolePrivate> privateRoleList = tenantRolePrivateService.list(Wrappers.<TenantRolePrivate>lambdaQuery()
                    .eq(TenantRolePrivate::getStatus, CommonStatusEnum.ENABLE)
                    .in(TenantRolePrivate::getId, privateRoleIds));
            result.addAll(privateRoleList);
        }

        return result;
    }

    @Override
    public void setUserRoles(long userId, List<Long> roleIds) {
        // 如果操作的自己，那么需要判断角色
        long opsUserId = SecurityAuthContext.getUser().getId();
        if (opsUserId == userId) {
            BizRoleUtils.ensureRoles(userId, roleIds, RoleConstants.ROLE_ADMIN_CODE,
                    bizRoleService, tenantRoleUserPrivateService);
            BizRoleUtils.ensureRoles(userId, roleIds, RoleConstants.ROLE_ORG_ADMIN_CODE,
                    bizRoleService, tenantRoleUserPrivateService);
        }

        List<RoleType> roles = bizRoleService.getRoles(roleIds);
        List<BizAssignRoleBO> assignRoles = roles.stream()
                .map(item -> {
                    BizAssignRoleBO role = new BizAssignRoleBO();
                    role.setRoleId(item.getId());
                    role.setMetaRole(item.getOrgType() == OrgTypeEnum.Platform);
                    return role;
                }).toList();

        tenantRoleUserPrivateService.setRoles(userId, assignRoles);
    }

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

        user.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(sysUserService.updateById(user),
                "SysUserServiceImpl.UpdateFailed");
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
        sysUserService.create(user);

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
        sysUserService.update(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(long id) {
        userOpsChecker.removeUser(id);

        // 取消关联角色
        tenantRoleUserPrivateService.clearByUserId(id);

        // 取消关联社交信息
        sysUserSocialService.remove(
                Wrappers.<SysUserSocial>lambdaQuery().eq(SysUserSocial::getUserId, id));

        // 取消关联租户
        sysUserTenantService.remove(
                Wrappers.<SysUserTenant>lambdaQuery().eq(SysUserTenant::getUserId, id));

        // 取消关联部门
        tenantUserDeptPrivateService.setDepartments(id, null);

        sysUserService.delete(id);
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
            setUserRoles(userId, params.getRoleIds());
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
            tenantUserDeptPrivateService.clearByUserId(userId);
            // 取消关联角色
            tenantRoleUserPrivateService.clearByUserId(userId);
        });
    }

    @Override
    public List<UserOrgInfoVO> userOrgInfo(long userId) {
        List<SysUserTenant> list = CollUtil.emptyIfNull(sysUserTenantService.getUserOrgs(userId));
        return list.stream().map(org ->
                TenantEnv.applyAs(org.getTenantId(), () -> {
                    UserOrgInfoVO item = new UserOrgInfoVO();
                    item.setOrgId(org.getTenantId());

                    List<Long> deptIds = getUserDeptIds(userId);
                    item.setDeptIds(deptIds);

                    List<Long> roleIds = CollUtil.emptyIfNull(getUserRoles(userId))
                            .stream().map(RoleType::getId).toList();
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
        profile.setDeptIds(getUserDeptIds(id));
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
            sysUserService.create(user);
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
        sysUserService.update(user);

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
        RoleType managerRole = bizRoleService.getByCode(RoleConstants.ROLE_ORG_ADMIN_CODE);
        long deleteCount = tenantRoleUserPrivateService.count(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getRoleId, managerRole.getId())
                .eq(TenantRoleUserPrivate::getUserId, id));
        assertionChecker.checkOperation(deleteCount == 0, "BizUserServiceImpl.CantRemoveManager");

        // 取消关联组织
        sysUserTenantService.leaveTenant(id);
        // 取消关联部门
        tenantUserDeptPrivateService.clearByUserId(id);
        // 取消关联角色
        tenantRoleUserPrivateService.clearByUserId(id);
    }

    @Override
    public void orgPasswordInit(UserPasswordDTO params) {
        long id = SecurityAuthContext.getUser().getId();
        SysUser current = sysUserService.getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;
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
