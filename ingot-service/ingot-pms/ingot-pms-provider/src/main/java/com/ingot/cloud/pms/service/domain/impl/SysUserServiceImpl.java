package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.api.model.status.PmsErrorCode;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;
    private final SysUserSocialService sysUserSocialService;
    private final SysUserTenantService sysUserTenantService;
    private final SysTenantService sysTenantService;
    private final SysUserDeptService sysUserDeptService;

    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertI18nService;
    private final UserTrans userTrans;

    @Override
    public UserInfoDTO getUserInfo(IngotUser user) {
        // 使用当前用户 tenant 进行操作
        return TenantEnv.applyAs(user.getTenantId(), () -> {
            SysUser userInfo = getById(user.getId());
            if (userInfo == null) {
                OAuth2ErrorUtils.throwInvalidRequest("用户异常");
            }

            SysUserDept userDept = sysUserDeptService.getByUserIdAndTenant(userInfo.getId(), user.getTenantId());
            List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), userDept.getDeptId());
            List<String> roleCodes = roles.stream()
                    .map(SysRole::getCode).collect(Collectors.toList());

            // 获取可以访问的租户列表
            List<SysUserTenant> userTenantList = sysUserTenantService.list(
                    Wrappers.<SysUserTenant>lambdaQuery()
                            .eq(SysUserTenant::getUserId, user.getId()));
            List<AllowTenantDTO> allows = BizUtils.getAllows(sysTenantService,
                    userTenantList.stream()
                            .map(SysUserTenant::getTenantId).collect(Collectors.toSet()),
                    (item) -> {
                        // main=true，为当前登录的租户
                        item.setMain(item.getId() == user.getTenantId());
                    });

            UserInfoDTO result = new UserInfoDTO();
            result.setUser(userTrans.toUserBaseInfo(userInfo));
            result.setRoles(roleCodes);
            result.setAllows(allows);
            return result;
        });
    }

    @Override
    public IPage<UserPageItemVO> conditionPage(Page<SysUser> page, UserQueryDTO condition) {
        Long tenantId = TenantContextHolder.get();
        return baseMapper.conditionPageWithTenant(page, condition, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(UserDTO params) {
        SysUser user = userTrans.to(params);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setCreatedAt(DateUtils.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }

        checkUserUniqueField(user, null);

        assertI18nService.checkOperation(save(user),
                "SysUserServiceImpl.CreateFailed");

        // 第一次创建为主要租户
        SysUserTenant userTenant = new SysUserTenant();
        userTenant.setUserId(user.getId());
        userTenant.setTenantId(TenantContextHolder.get());
        userTenant.setMain(Boolean.TRUE);
        userTenant.setCreatedAt(DateUtils.now());
        sysUserTenantService.save(userTenant);

        // 保存部门
        SysUserDept userDept = new SysUserDept();
        userDept.setUserId(user.getId());
        userDept.setTenantId(TenantContextHolder.get());
        userDept.setDeptId(params.getDeptId());
        sysUserDeptService.save(userDept);

        List<Long> roles = params.getRoleIds();
        if (CollUtil.isNotEmpty(roles)) {
            boolean result = roles.stream().allMatch(roleId -> {
                SysRoleUser entity = new SysRoleUser();
                entity.setUserId(user.getId());
                entity.setRoleId(roleId);
                return entity.insert();
            });
            assertI18nService.checkOperation(result,
                    "SysUserServiceImpl.CreateFailed");
        }

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserById(long id) {
        // 取消关联角色
        assertI18nService.checkOperation(sysRoleUserService.removeByUserId(id),
                "SysUserServiceImpl.RemoveFailed");

        // 取消关联社交信息
        assertI18nService.checkOperation(sysUserSocialService.remove(
                        Wrappers.<SysUserSocial>lambdaQuery().eq(SysUserSocial::getUserId, id)),
                "SysUserServiceImpl.RemoveFailed");

        // 取消关联租户
        assertI18nService.checkOperation(sysUserTenantService.remove(
                        Wrappers.<SysUserTenant>lambdaQuery().eq(SysUserTenant::getUserId, id)),
                "SysUserServiceImpl.RemoveFailed");

        // 取消关联部门
        assertI18nService.checkOperation(sysUserDeptService.remove(
                        Wrappers.<SysUserDept>lambdaQuery().eq(SysUserDept::getUserId, id)),
                "SysUserServiceImpl.RemoveFailed");

        assertI18nService.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserDTO params) {
        long userId = params.getId();
        SysUser current = getById(userId);
        SysUser user = userTrans.to(params);
        if (StrUtil.isNotEmpty(params.getNewPassword())) {
            user.setPassword(passwordEncoder.encode(params.getNewPassword()));
            user.setInitPwd(false);
        }

        checkUserUniqueField(user, current);

        user.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");

        if (CollUtil.isNotEmpty(params.getRoleIds())) {
            // 更新角色
            sysRoleUserService.updateUserRole(userId, params.getRoleIds());
        }
    }

    private void checkUserUniqueField(SysUser update, SysUser current) {
        // 更新字段不为空，并且不等于当前值
        if (StrUtil.isNotEmpty(update.getUsername())
                && (current == null || !StrUtil.equals(update.getUsername(), current.getUsername()))) {
            assertI18nService.checkBiz(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, update.getUsername())) == 0,
                    PmsErrorCode.ExistUsername.getCode(),
                    "SysUserServiceImpl.UsernameExist");
        }

        if (StrUtil.isNotEmpty(update.getPhone())
                && (current == null || !StrUtil.equals(update.getPhone(), current.getPhone()))) {
            assertI18nService.checkBiz(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, update.getPhone())) == 0,
                    PmsErrorCode.ExistPhone.getCode(),
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(update.getEmail())
                && (current == null || !StrUtil.equals(update.getEmail(), current.getEmail()))) {
            assertI18nService.checkBiz(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, update.getEmail())) == 0,
                    PmsErrorCode.ExistEmail.getCode(),
                    "SysUserServiceImpl.EmailExist");
        }
    }


    @Override
    public void fixPassword(long id, UserPasswordDTO params) {
        SysUser current = getById(id);
        assertI18nService.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        assertI18nService.checkOperation(passwordEncoder.matches(params.getPassword(), current.getPassword()),
                "SysUserServiceImpl.IncorrectPassword");
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setInitPwd(false);
        assertI18nService.checkOperation(user.updateById(),
                "SysUserServiceImpl.UpdatePasswordFailed");
    }

}
