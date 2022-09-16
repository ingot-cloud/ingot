package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;

    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final AssertI18nService assertI18nService;
    private final UserTrans userTrans;

    @Override
    public UserInfoDTO getUserInfo(IngotUser user) {
        // 使用当前用户 tenant 进行操作
        return TenantEnv.applyAs(user.getTenantId(), () -> {
            SysUser userInfo = getById(user.getId());
            if (userInfo == null) {
                OAuth2ErrorUtils.throwInvalidRequest("用户异常");
            }

            List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
            List<String> roleCodes = roles.stream()
                    .map(SysRole::getCode).collect(Collectors.toList());

            UserInfoDTO result = new UserInfoDTO();
            result.setUser(userInfo);
            result.setRoles(roleCodes);
            return result;
        });
    }

    @Override
    public IPage<UserPageItemVO> conditionPage(Page<SysUser> page, UserDTO condition) {
        List<Long> clientIds = condition.getClientIds();
        // 如果客户端ID不为空，那么查询客户端拥有的所有角色，和condition中的角色列表合并
        if (CollUtil.isNotEmpty(clientIds)) {
            Set<Long> roleIds = sysRoleService.getAllRolesOfClients(clientIds)
                    .stream().map(SysRole::getId).collect(Collectors.toSet());
            if (CollUtil.isNotEmpty(roleIds)) {
                List<Long> currentRoleIds = condition.getRoleIds();
                if (CollUtil.isNotEmpty(currentRoleIds)) {
                    roleIds.addAll(currentRoleIds);
                }
                condition.setRoleIds(CollUtil.newArrayList(roleIds));
            }
        }

        return baseMapper.conditionPage(page, condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserDTO params) {
        SysUser user = userTrans.to(params);
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setId(idGenerator.nextId());
        user.setCreatedAt(DateUtils.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }

        if (StrUtil.isNotEmpty(user.getPhone())) {
            assertI18nService.checkOperation(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, user.getPhone())) == 0,
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())) {
            assertI18nService.checkOperation(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, user.getEmail())) == 0,
                    "SysUserServiceImpl.EmailExist");
        }

        assertI18nService.checkOperation(save(user),
                "SysUserServiceImpl.CreateFailed");

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserById(long id) {
        // 取消关联角色
        assertI18nService.checkOperation(sysRoleUserService.removeByUserId(id),
                "SysUserServiceImpl.RemoveFailed");

        assertI18nService.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserDTO params) {
        long userId = params.getId();
        SysUser user = userTrans.to(params);
        if (StrUtil.isNotEmpty(params.getNewPassword())) {
            user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        }

        if (StrUtil.isNotEmpty(user.getPhone())) {
            assertI18nService.checkOperation(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, user.getPhone())
                            .ne(SysUser::getId, userId)) == 0,
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())) {
            assertI18nService.checkOperation(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, user.getEmail())
                            .ne(SysUser::getId, userId)) == 0,
                    "SysUserServiceImpl.EmailExist");
        }

        user.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");

        if (CollUtil.isNotEmpty(params.getRoleIds())) {
            // 更新角色
            sysRoleUserService.updateUserRole(userId, params.getRoleIds());
        }
    }

    @Override
    public void updateUserBaseInfo(long id, UserBaseInfoDTO params) {
        SysUser current = getById(id);
        assertI18nService.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        SysUser user = userTrans.to(params);
        if (StrUtil.isNotEmpty(user.getPhone())
                && !StrUtil.equals(user.getPhone(), current.getPhone())) {
            assertI18nService.checkOperation(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, user.getPhone())) == 0,
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(user.getEmail())
                && !StrUtil.equals(user.getEmail(), current.getEmail())) {
            assertI18nService.checkOperation(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, user.getEmail())) == 0,
                    "SysUserServiceImpl.EmailExist");
        }

        user.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");
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
        assertI18nService.checkOperation(user.updateById(),
                "SysUserServiceImpl.UpdatePasswordFailed");
    }

    @Override
    public UserProfileVO getUserProfile(long id) {
        SysUser user = getById(id);
        assertI18nService.checkOperation(user != null,
                "SysUserServiceImpl.UserNonExist");
        assert user != null;

        UserProfileVO profile = userTrans.to(user);

        List<SysRole> list = sysRoleService.getRolesOfUser(id);
        if (CollUtil.isNotEmpty(list)) {
            profile.setRoleIds(list.stream()
                    .map(SysRole::getId).collect(Collectors.toList()));
        }

        return profile;
    }

    @Override
    public boolean matchDept(long deptId) {
        long count = count(lambdaQuery().eq(SysUser::getDeptId, deptId));
        return count > 0;
    }

    @Override
    public boolean anyMatchDept(List<Long> deptIds) {
        List<Long> ids = Optional.ofNullable(deptIds).orElse(CollUtil.newArrayList());
        long count = count(lambdaQuery().in(SysUser::getDeptId, deptIds));
        return count > 0;
    }
}
