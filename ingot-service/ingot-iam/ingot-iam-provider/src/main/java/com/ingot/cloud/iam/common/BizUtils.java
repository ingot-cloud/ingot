package com.ingot.cloud.iam.common;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.convert.RoleConvert;
import com.ingot.cloud.iam.api.model.domain.SysTenant;
import com.ingot.cloud.iam.api.model.domain.SysUser;
import com.ingot.cloud.iam.api.model.status.IamErrorCode;
import com.ingot.cloud.iam.api.model.types.RoleType;
import com.ingot.cloud.iam.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.iam.service.domain.SysTenantService;
import com.ingot.cloud.iam.service.domain.SysUserService;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.core.utils.validation.AssertionChecker;

/**
 * <p>Description  : BizUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/14.</p>
 * <p>Time         : 10:38 AM.</p>
 */
public class BizUtils {

    /**
     * 获取 {@link TenantMainDTO} 列表
     */
    public static List<TenantMainDTO> getTenants(SysTenantService sysTenantService,
                                                 Set<Long> userTenantList,
                                                 Consumer<TenantMainDTO> mainConsumer) {
        return sysTenantService.list(
                        Wrappers.<SysTenant>lambdaQuery()
                                .in(SysTenant::getId, userTenantList))
                .stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .map(item -> {
                    TenantMainDTO dto = new TenantMainDTO();
                    dto.setId(String.valueOf(item.getId()));
                    dto.setName(item.getName());
                    dto.setAvatar(item.getAvatar());
                    mainConsumer.accept(dto);
                    return dto;
                })
                .toList();
    }

    /**
     * 根据当前用户状态和可访问租户列表，返回用户最终状态
     */
    public static UserStatusEnum getUserStatus(List<TenantMainDTO> allows, UserStatusEnum userStatus, Long loginTenant) {
        // 没有允许访问的租户，那么直接返回不可用
        if (CollUtil.isEmpty(allows)) {
            return UserStatusEnum.LOCK;
        }
        // 如果允许访问的tenant中不存在当前登录的tenant，那么直接返回不可用
        if (loginTenant != null && allows.stream().noneMatch(item -> Long.parseLong(item.getId()) == loginTenant)) {
            return UserStatusEnum.LOCK;
        }
        return userStatus;
    }

    /**
     * 转换RoleType为RoleTreeNodeVO
     *
     * @param role        {@link RoleType}
     * @param roleConvert {@link RoleConvert}
     * @return {@link RoleTreeNodeVO}
     */
    public static RoleTreeNodeVO convert(RoleType role, RoleConvert roleConvert) {
        RoleTreeNodeVO item = roleConvert.to(role);
        item.setTypeText(role.getType().getText());
        item.setOrgTypeText(role.getOrgType().getText());
        item.setStatusText(role.getStatus().getText());
        return item;
    }

    /**
     * 检测用户唯一字段
     *
     * @param update           更新用户
     * @param current          当前用户
     * @param service          用户服务
     * @param assertionChecker checker
     */
    public static void checkUserUniqueField(SysUser update, SysUser current, SysUserService service, AssertionChecker assertionChecker) {
        // 更新字段不为空，并且不等于当前值
        if (StrUtil.isNotEmpty(update.getUsername())
                && (current == null || !StrUtil.equals(update.getUsername(), current.getUsername()))) {
            assertionChecker.checkBiz(service.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, update.getUsername())) == 0,
                    IamErrorCode.ExistUsername.getCode(),
                    "SysUserServiceImpl.UsernameExist");
        }

        if (StrUtil.isNotEmpty(update.getPhone())
                && (current == null || !StrUtil.equals(update.getPhone(), current.getPhone()))) {
            assertionChecker.checkBiz(service.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, update.getPhone())) == 0,
                    IamErrorCode.ExistPhone.getCode(),
                    "SysUserServiceImpl.PhoneExist");
        }

        if (StrUtil.isNotEmpty(update.getEmail())
                && (current == null || !StrUtil.equals(update.getEmail(), current.getEmail()))) {
            assertionChecker.checkBiz(service.count(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getEmail, update.getEmail())) == 0,
                    IamErrorCode.ExistEmail.getCode(),
                    "SysUserServiceImpl.EmailExist");
        }
    }
}
