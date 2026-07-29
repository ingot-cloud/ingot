package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.AllOrgUserFilterDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.port.inbound.DeleteAccountUseCase;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final AssertionChecker assertionChecker;

    private final RegisterUserUseCase registerUserUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;

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
        assertionChecker.checkOperation(StrUtil.isNotEmpty(user.getUsername()),
                "SysUserServiceImpl.UsernameNonNull");

        checkUserUniqueField(user, null);

        UserAccount account = registerUserUseCase.register(RegisterUserUseCase.RegisterUserCommand.builder()
                .creationSource(RegisterUserUseCase.CreationSource.ADMIN_CREATE)
                .username(user.getPhone())
                .password(user.getPassword())
                .userType(UserTypeEnum.ADMIN)
                .phone(user.getPhone())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .mustChangePwd(Boolean.TRUE)
                .createdBy(SecurityAuthContext.getUser().getId())
                .build());

        user.setId(account.getId());
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
        InUser operator = SecurityAuthContext.getUser();
        deleteAccountUseCase.deleteAccount(DeleteAccountUseCase.DeleteAccountCommand.builder()
                .userId(id)
                .userType(UserTypeEnum.ADMIN)
                .source(EventSource.PMS)
                .operatorId(operator.getId())
                .operatorName(operator.getUsername())
                .build());
    }

    private void checkUserUniqueField(SysUser update, SysUser current) {
        BizUtils.checkUserUniqueField(update, current, this, assertionChecker);
    }
}
