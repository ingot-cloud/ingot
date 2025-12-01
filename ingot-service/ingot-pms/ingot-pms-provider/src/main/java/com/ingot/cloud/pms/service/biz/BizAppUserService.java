package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.Member;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;

/**
 * <p>Description  : BizAppUserService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 14:06.</p>
 */
public interface BizAppUserService {
    /**
     * 用户列表分页
     *
     * @param page   分页参数
     * @param filter 条件参数
     * @return {@link Member}
     */
    IPage<Member> page(Page<Member> page, Member filter);

    /**
     * 用户列表分页
     *
     * @param page   分页参数
     * @param filter 条件参数
     * @return {@link Member}
     */
    IPage<Member> pageTenant(Page<Member> page, Member filter);

    /**
     * 获取用户简介信息
     *
     * @param id 用户ID
     * @return {@link UserProfileVO}
     */
    UserProfileVO getUserProfile(long id);

    /**
     * 更新用户基本信息
     *
     * @param params 基本信息参数
     */
    void updateUserBaseInfo(long id, UserBaseInfoDTO params);

    /**
     * 创建用户, 如果手机号已经创建用户那么不处理
     *
     * @param params {@link AppUserCreateDTO}
     * @return {@link Member}
     */
    Member createIfPhoneNotUsed(AppUserCreateDTO params);

    /**
     * 创建用户
     *
     * @param params {@link AppUserCreateDTO}
     * @return {@link ResetPwdVO} 初始化密码
     */
    ResetPwdVO createUser(AppUserCreateDTO params);

    /**
     * 用户更新操作
     *
     * @param params
     */
    void updateUser(Member params);

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
     * 用户组织信息编辑，如果用户没加入该组织，那么直接加入组织
     *
     * @param params {@link UserOrgEditDTO}
     */
    void userOrgEdit(UserOrgEditDTO params);

    /**
     * 用户离开组织
     *
     * @param params {@link UserOrgEditDTO}
     */
    void userOrgLeave(UserOrgEditDTO params);

    /**
     * 用户组织信息
     *
     * @param userId 用户ID
     * @return {@link UserOrgInfoVO}
     */
    List<UserOrgInfoVO> userOrgInfo(long userId);

    /**
     * 获取用户简介信息
     *
     * @param id 用户ID
     * @return {@link UserProfileVO}
     */
    OrgUserProfileVO getOrgUserProfile(long id);

    /**
     * 组织创建用户
     *
     * @param params {@link OrgUserDTO}
     */
    void orgCreateUser(OrgUserDTO params);

    /**
     * 组织更新用户
     *
     * @param params {@link OrgUserDTO}
     */
    void orgUpdateUser(OrgUserDTO params);

    /**
     * 组织删除用户
     *
     * @param id ID
     */
    void orgDeleteUser(long id);

    /**
     * 密码初始化
     *
     * @param params {@link UserPasswordDTO}
     */
    void orgPasswordInit(UserPasswordDTO params);

    /**
     * 修改密码
     *
     * @param params {@link UserPasswordDTO}
     */
    void fixPassword(UserPasswordDTO params);
}
