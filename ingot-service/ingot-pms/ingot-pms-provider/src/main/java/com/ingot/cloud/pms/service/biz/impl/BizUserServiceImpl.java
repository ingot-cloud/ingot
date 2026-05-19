package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;
import java.util.Objects;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.bo.role.BizAssignRoleBO;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.*;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemWithBindRoleStatusVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.core.BizRoleUtils;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserDeptService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.biz.UserOpsChecker;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.account.domain.model.enums.EventSource;
import com.ingot.framework.account.domain.model.enums.LockReason;
import com.ingot.framework.account.domain.port.inbound.*;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.utils.PageUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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

    private final PlatformRoleService platformRoleService;
    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;
    private final BizRoleService bizRoleService;
    private final BizUserDeptService bizUserDeptService;

    private final ChangePasswordUseCase changePasswordUseCase;
    private final ManageAccountStatusUseCase manageAccountStatusUseCase;
    private final LockAccountUseCase lockAccountUseCase;
    private final UnlockAccountUseCase unlockAccountUseCase;
    private final AssertionChecker assertionChecker;
    private final UserOpsChecker userOpsChecker;
    private final UserConvert userConvert;

    @Override
    public IPage<UserPageItemWithBindRoleStatusVO> conditionPageWithRole(Page<SysUser> page,
                                                                         UserQueryDTO condition,
                                                                         Long orgId,
                                                                         Long roleId) {
        List<Long> userIds = tenantRoleUserPrivateService.listRoleUsers(roleId)
                .stream()
                .map(TenantRoleUserPrivate::getUserId)
                .toList();
        return PageUtils.map(sysUserService.conditionPage(page, condition, orgId), item -> {
            UserPageItemWithBindRoleStatusVO vo = new UserPageItemWithBindRoleStatusVO();
            BeanUtil.copyProperties(item, vo);
            vo.setCanBind(userIds.stream().noneMatch(id -> Objects.equals(id, item.getUserId())));
            return vo;
        });
    }

    @Override
    public List<RoleType> getUserRoles(long userId) {
        return BizRoleUtils.getUserRoles(userId,
                platformRoleService, tenantRoleUserPrivateService, tenantRolePrivateService);
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
        // 不能绑定角色组
        assertionChecker.checkOperation(roles.stream()
                        .noneMatch(item -> item.getType() == RoleTypeEnum.GROUP),
                "BizUserServiceImpl.CantBindRoleGroup");

        List<BizAssignRoleBO> assignRoles = roles.stream()
                .map(item -> {
                    BizAssignRoleBO role = new BizAssignRoleBO();
                    role.setRoleId(item.getId());
                    role.setPlatformRole(item.getPlatformRole());
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
        if (StrUtil.isEmpty(params.getUsername())) {
            params.setUsername(params.getPhone());
        }

        SysUser user = userConvert.to(params);
        // 默认初始化密码
        String initPwd = RandomUtil.randomString(8);
        user.setPassword(initPwd);

        sysUserService.create(user);

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(initPwd);
        result.setId(user.getId());
        return result;
    }

    @Override
    public void updateUser(UserDTO params) {
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
        bizUserDeptService.clear(id);

        sysUserService.delete(id);
    }

    @Override
    public ResetPwdVO resetPwd(long userId) {
        InUser operator = SecurityAuthContext.getUser();
        String randomPwd = RandomUtil.randomString(6);

        changePasswordUseCase.resetPassword(ChangePasswordUseCase.ResetPasswordCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.ADMIN)
                .newPassword(randomPwd)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.PMS)
                .build());

        ResetPwdVO result = new ResetPwdVO();
        result.setRandom(randomPwd);
        return result;
    }

    @Override
    public void userOrgEdit(UserOrgEditDTO params) {
        TenantEnv.runAs(params.getOrgId(), () -> {
            long userId = params.getId();
            List<Long> roleIds = params.getRoleIds();
            SysTenant tenant = sysTenantService.getById(params.getOrgId());

            sysUserTenantService.joinTenant(userId, tenant);
            bizUserDeptService.setDepts(userId, params.getDeptIds());

            // 直接给组织人员配置角色，不能配置部门角色
            if (CollUtil.isNotEmpty(roleIds)) {
                List<RoleType> roles = bizRoleService.getRoles(roleIds);
                // 不能绑定部门角色
                assertionChecker.checkOperation(roles.stream()
                                .noneMatch(item -> BooleanUtil.isTrue(item.getFilterDept())),
                        "BizUserServiceImpl.CantBindDeptRole");
                setUserRoles(userId, roleIds);
            }
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
            bizUserDeptService.clear(userId);
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

                    List<Long> deptIds = bizUserDeptService.getDeptIds(userId);
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
        profile.setDeptIds(bizUserDeptService.getDeptIds(id));
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
            sysUserService.create(user);
        }

        // 加入租户
        SysTenant tenant = sysTenantService.getById(TenantContextHolder.get());
        sysUserTenantService.joinTenant(user.getId(), tenant);
        // 设置部门
        bizUserDeptService.setDepts(user.getId(), params.getDeptIds());
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
            bizUserDeptService.setDepts(user.getId(), params.getDeptIds());
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
        bizUserDeptService.clear(id);
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
        if (!BooleanUtil.isTrue(current.getMustChangePwd())) {
            return;
        }

        changePasswordUseCase.forceChangePassword(ChangePasswordUseCase.ForceChangePasswordCommand.builder()
                .userId(id)
                .userType(UserTypeEnum.ADMIN)
                .newPassword(params.getNewPassword())
                .source(EventSource.PMS)
                .build());
    }

    @Override
    public void fixPassword(UserPasswordDTO params) {
        long id = SecurityAuthContext.getUser().getId();
        changePasswordUseCase.changePassword(ChangePasswordUseCase.ChangePasswordCommand.builder()
                .userId(id)
                .userType(UserTypeEnum.ADMIN)
                .oldPassword(params.getPassword())
                .newPassword(params.getNewPassword())
                .confirmPassword(params.getNewPassword())
                .build());
    }

    @Override
    public void enableAccount(long userId, @Nullable String reason) {
        InUser operator = SecurityAuthContext.getUser();
        manageAccountStatusUseCase.enableAccount(ManageAccountStatusUseCase.StatusCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.ADMIN)
                .targetStatus(Boolean.TRUE)
                .reason(reason)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.PMS)
                .build());
    }

    @Override
    public void disableAccount(long userId, @Nullable String reason) {
        userOpsChecker.disableUser(userId);
        InUser operator = SecurityAuthContext.getUser();
        manageAccountStatusUseCase.disableAccount(ManageAccountStatusUseCase.StatusCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.ADMIN)
                .targetStatus(Boolean.FALSE)
                .reason(reason)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.PMS)
                .build());
    }

    @Override
    public void lockAccount(long userId, AccountLockDTO params) {
        InUser operator = SecurityAuthContext.getUser();
        lockAccountUseCase.lockManually(LockAccountUseCase.LockCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.ADMIN)
                .reason(LockReason.MANUAL_LOCK)
                .reasonDetail(params.getReasonDetail())
                .lockedUntil(params.getLockedUntil())
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.PMS)
                .build());
    }

    @Override
    public void unlockAccount(long userId, @Nullable String reason) {
        InUser operator = SecurityAuthContext.getUser();
        unlockAccountUseCase.unlockManually(UnlockAccountUseCase.UnlockCommand.builder()
                .userId(userId)
                .userType(UserTypeEnum.ADMIN)
                .reason(reason)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .source(EventSource.PMS)
                .build());
    }
}
