package com.ingot.cloud.member.service.biz;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.dto.user.MemberUserBaseInfoDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserPasswordDTO;
import com.ingot.cloud.member.api.model.vo.user.MemberUserProfileVO;
import com.ingot.framework.commons.model.security.ResetPwdVO;

/**
 * <p>Description  : BizUserService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 11:21.</p>
 */
public interface BizUserService {

    /**
     * 条件分页
     *
     * @param page      分页参数
     * @param condition 查询参数
     * @return {@link MemberUser}
     */
    IPage<MemberUser> conditionPage(Page<MemberUser> page, MemberUserDTO condition);

    /**
     * 获取用户绑定的所有角色
     *
     * @param userId 用户ID
     * @return {@link MemberRole}
     */
    List<MemberRole> getUserRoles(long userId);

    /**
     * 设置用户角色
     *
     * @param userId  用户
     * @param roleIds 角色列表
     */
    void setUserRoles(long userId, List<Long> roleIds);

    /**
     * 获取用户简介信息
     *
     * @param id 用户ID
     * @return {@link MemberUserProfileVO}
     */
    MemberUserProfileVO getUserProfile(long id);

    /**
     * 更新用户基本信息
     *
     * @param params 基本信息参数
     */
    void updateUserBaseInfo(long id, MemberUserBaseInfoDTO params);

    /**
     * 创建用户
     *
     * @param params {@link MemberUserDTO}
     * @return {@link ResetPwdVO} 初始化密码
     */
    ResetPwdVO createUser(MemberUserDTO params);

    /**
     * 更新用户
     *
     * @param params {@link MemberUserDTO}
     */
    void updateUser(MemberUserDTO params);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(long id);

    /**
     * 重置密码
     *
     * @param userId 用户ID
     * @return {@link ResetPwdVO} 初始化密码
     */
    ResetPwdVO resetPwd(long userId);

    /**
     * 修改密码
     *
     * @param params {@link MemberUserPasswordDTO}
     */
    void fixPassword(MemberUserPasswordDTO params);
}
