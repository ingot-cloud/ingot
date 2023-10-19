package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
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
