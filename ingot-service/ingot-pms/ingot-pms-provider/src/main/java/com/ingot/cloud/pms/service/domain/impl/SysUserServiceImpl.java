package com.ingot.cloud.pms.service.domain.impl;

import java.time.LocalDateTime;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.AllOrgUserFilterDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.api.model.status.PmsErrorCode;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
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
    private final CredentialSecurityService credentialSecurityService;
    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;

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
    public void create(SysUser user) {
        user.setInitPwd(Boolean.TRUE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(DateUtil.now());
        if (user.getStatus() == null) {
            user.setStatus(UserStatusEnum.ENABLE);
        }

        checkUserUniqueField(user, null);

        assertionChecker.checkOperation(save(user),
                "SysUserServiceImpl.CreateFailed");
    }

    @Override
    public void update(SysUser user) {
        SysUser current = getById(user.getId());

        checkUserUniqueField(user, current);
        user.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");
    }

    @Override
    public void delete(long id) {
        assertionChecker.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    private void checkUserUniqueField(SysUser update, SysUser current) {
        // 更新字段不为空，并且不等于当前值
        if (StrUtil.isNotEmpty(update.getUsername())
                && (current == null || !StrUtil.equals(update.getUsername(), current.getUsername()))) {
            assertionChecker.checkBiz(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, update.getUsername())) == 0,
                    PmsErrorCode.ExistUsername.getCode(),
                    "SysUserServiceImpl.UsernameExist");
        }

        if (StrUtil.isNotEmpty(update.getPhone())
                && (current == null || !StrUtil.equals(update.getPhone(), current.getPhone()))) {
            assertionChecker.checkBiz(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, update.getPhone())) == 0,
                    PmsErrorCode.ExistPhone.getCode(),
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(update.getEmail())
                && (current == null || !StrUtil.equals(update.getEmail(), current.getEmail()))) {
            assertionChecker.checkBiz(count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, update.getEmail())) == 0,
                    PmsErrorCode.ExistEmail.getCode(),
                    "SysUserServiceImpl.EmailExist");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fixPassword(long id, UserPasswordDTO params) {
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPassword())
                        && StrUtil.isNotEmpty(params.getNewPassword()),
                "SysUserServiceImpl.IncorrectPassword");

        SysUser current = getById(id);
        assertionChecker.checkOperation(current != null,
                "SysUserServiceImpl.UserNonExist");
        assert current != null;

        assertionChecker.checkOperation(passwordEncoder.matches(params.getPassword(), current.getPassword()),
                "SysUserServiceImpl.IncorrectPassword");

        updatePassword(id, params.getNewPassword(), false);
    }

    @Override
    public void updatePassword(long id, String password, boolean initFlag) {
        credentialSecurityService.validate(CredentialValidateRequest.builder()
                .scene(CredentialScene.CHANGE_PASSWORD)
                .userId(id)
                .password(password)
                .build());

        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        user.setInitPwd(initFlag);
        user.setUpdatedAt(LocalDateTime.now());
        assertionChecker.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdatePasswordFailed");
    }

}
