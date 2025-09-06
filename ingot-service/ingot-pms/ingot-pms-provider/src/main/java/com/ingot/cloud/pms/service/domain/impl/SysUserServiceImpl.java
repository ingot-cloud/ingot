package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.user.AllOrgUserFilterDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.api.model.status.PmsErrorCode;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
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
    private final SysDeptService sysDeptService;

    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertI18nService;
    private final UserConvert userConvert;
    private final TenantProperties tenantProperties;

    @Override
    public UserInfoDTO getUserInfo(InUser user) {
        // 使用当前用户 tenant 进行操作
        return TenantEnv.applyAs(user.getTenantId(), () -> {
            SysUser userInfo = getById(user.getId());
            if (userInfo == null) {
                OAuth2ErrorUtils.throwInvalidRequest("用户异常");
            }

            List<SysRole> roles = sysRoleService.getRolesOfUser(user.getId());
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
                        item.setMain(Long.parseLong(item.getId()) == user.getTenantId());
                    });

            UserInfoDTO result = new UserInfoDTO();
            result.setUser(userConvert.toUserBaseInfo(userInfo));
            result.setRoles(roleCodes);
            result.setAllows(allows);
            return result;
        });
    }

    @Override
    public List<Long> getAdminIdList() {
        return TenantEnv.applyAs(tenantProperties.getDefaultId(), () -> {
            SysRole admin = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getCode, RoleConstants.ROLE_ADMIN_CODE));
            if (admin == null) {
                return ListUtil.empty();
            }

            return sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                            .eq(SysRoleUser::getRoleId, admin.getId()))
                    .stream().map(SysRoleUser::getUserId).toList();
        });
    }

    @Override
    public IPage<UserPageItemVO> conditionPage(Page<SysUser> page, UserQueryDTO condition, Long orgId) {
        return baseMapper.conditionPageWithTenant(page, condition, orgId);
    }

    @Override
    public IPage<UserPageItemVO> pageByDept(Page<SysUser> page, Long deptId, Long orgId) {
        return baseMapper.pageByDept(page, deptId, orgId);
    }

    @Override
    public IPage<SysUser> allOrgUserPage(Page<SysUser> page, AllOrgUserFilterDTO filter) {
        // 查询系统所有组织用户，不进行数据隔离
        return TenantEnv.applyAs(null, () ->
                page(page, Wrappers.<SysUser>lambdaQuery()
                        .like(StrUtil.isNotEmpty(filter.getPhone()), SysUser::getPhone, filter.getPhone())
                        .like(StrUtil.isNotEmpty(filter.getNickname()), SysUser::getNickname, filter.getNickname())
                        .like(StrUtil.isNotEmpty(filter.getEmail()), SysUser::getEmail, filter.getEmail()))
        );
    }

    @Override
    public void createUser(SysUser user) {
        user.setInitPwd(Boolean.TRUE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(DateUtil.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }

        checkUserUniqueField(user, null);

        assertI18nService.checkOperation(save(user),
                "SysUserServiceImpl.CreateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserById(long id) {
        // 取消关联角色
        sysRoleUserService.removeByUserId(id);

        // 取消关联社交信息
        sysUserSocialService.remove(
                Wrappers.<SysUserSocial>lambdaQuery().eq(SysUserSocial::getUserId, id));

        // 取消关联租户
        sysUserTenantService.remove(
                Wrappers.<SysUserTenant>lambdaQuery().eq(SysUserTenant::getUserId, id));

        // 取消关联部门
        sysDeptService.setDepts(id, null);

        assertI18nService.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    @Override
    public void updateUser(SysUser user) {
        SysUser current = getById(user.getId());

        if (StrUtil.isNotEmpty(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            // 如果没有传递init pwd，那么设置为false
            if (user.getInitPwd() == null) {
                user.setInitPwd(Boolean.FALSE);
            }
        }

        checkUserUniqueField(user, current);

        user.setUpdatedAt(DateUtil.now());
        assertI18nService.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");
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
        assertI18nService.checkOperation(StrUtil.isNotEmpty(params.getPassword())
                        && StrUtil.isNotEmpty(params.getNewPassword()),
                "SysUserServiceImpl.IncorrectPassword");

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
