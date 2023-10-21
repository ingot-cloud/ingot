package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.vo.biz.ResetPwdVO;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.framework.security.core.userdetails.IngotUser;

import java.util.List;

/**
 * <p>Description  : 业务用户service.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 6:44 PM.</p>
 */
public interface BizUserService {

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
     * 获取用户可用菜单
     *
     * @param user {@link IngotUser}
     * @return {@link MenuTreeNodeVO} List
     */
    List<MenuTreeNodeVO> getUserMenus(IngotUser user);

    /**
     * 创建用户
     *
     * @param params {@link UserDTO}
     * @return {@link ResetPwdVO} 初始化密码
     */
    ResetPwdVO createUser(UserDTO params);

    /**
     * 更新用户
     *
     * @param params {@link UserDTO}
     */
    void updateUser(UserDTO params);

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
