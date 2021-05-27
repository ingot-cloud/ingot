package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDto;
import com.ingot.cloud.pms.api.model.dto.user.UserDto;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDto;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVo;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.cloud.pms.service.SysRoleUserService;
import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.component.id.IdGenerator;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.I18nService;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.exception.UnauthorizedException;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
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
@AllArgsConstructor
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;

    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final I18nService i18nService;
    private final UserTrans userTrans;

    @Override
    public UserInfoDto getUserInfo(IngotUser user) {
        SysUser userInfo = getById(user.getId());
        if (userInfo == null) {
            throw new UnauthorizedException("用户异常");
        }

        List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode).collect(Collectors.toList());

        UserInfoDto result = new UserInfoDto();
        result.setUser(userInfo);
        result.setRoles(roleCodes);
        return result;
    }

    @Override
    public IPage<UserPageItemVo> conditionPage(Page<SysUser> page, UserDto condition) {
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
    public void createUser(UserDto params) {
        SysUser user = userTrans.to(params);
        user.setId(idGenerator.nextId());
        user.setCreatedAt(DateUtils.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }
        AssertionUtils.checkOperation(save(user),
                i18nService.getMessage("SysUserServiceImpl.CreateFailed"));

        List<Long> roles = params.getRoleIds();
        if (CollUtil.isNotEmpty(roles)) {
            boolean result = roles.stream().allMatch(roleId -> {
                SysRoleUser entity = new SysRoleUser();
                entity.setUserId(user.getId());
                entity.setRoleId(roleId);
                return entity.insert();
            });
            AssertionUtils.checkOperation(result,
                    i18nService.getMessage("SysUserServiceImpl.CreateFailed"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserById(long id) {
        AssertionUtils.checkOperation(sysRoleUserService.removeByUserId(id),
                "SysUserServiceImpl.RemoveFailed");
        AssertionUtils.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserDto params) {
        long userId = params.getId();
        SysUser user = userTrans.to(params);
        if (StrUtil.isNotEmpty(params.getNewPassword())) {
            user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        }
        user.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(user),
                i18nService.getMessage("SysUserServiceImpl.UpdateFailed"));

        // 更新角色
        AssertionUtils.checkOperation(sysRoleUserService.updateUserRole(userId, null),
                i18nService.getMessage("SysUserServiceImpl.UpdateFailed"));
    }

    @Override
    public void updateUserBaseInfo(long id, UserBaseInfoDto params) {
        SysUser current = getById(id);
        AssertionUtils.checkOperation(current != null,
                i18nService.getMessage("SysUserServiceImpl.UserNonExist"));

        SysUser user = userTrans.to(params);
        if (StrUtil.isNotEmpty(user.getPassword())) {
            AssertionUtils.checkOperation(passwordEncoder.matches(user.getPassword(), current.getPassword()),
                    i18nService.getMessage("SysUserServiceImpl.IncorrectPassword"));
        }

        user.setUpdatedAt(DateUtils.now());
        AssertionUtils.checkOperation(updateById(user),
                i18nService.getMessage("SysUserServiceImpl.UpdateFailed"));
    }
}
