package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.biz.UserOrgEditDTO;
import com.ingot.cloud.pms.api.model.dto.user.*;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.framework.commons.model.security.ResetPwdVO;
import com.ingot.cloud.pms.api.model.vo.biz.UserOrgInfoVO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemWithBindRoleStatusVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;

/**
 * <p>Description  : 业务用户service.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 6:44 PM.</p>
 */
public interface BizUserService {

    /**
     * 获取用户，并且返回是否可以绑定指定角色状态
     *
     * @param page      分页数据
     * @param condition 条件参数
     * @param orgId     组织ID
     * @param roleId    指定角色ID
     * @return {@link UserPageItemWithBindRoleStatusVO}
     */
    IPage<UserPageItemWithBindRoleStatusVO> conditionPageWithRole(Page<SysUser> page, UserQueryDTO condition, Long orgId, Long roleId);

    /**
     * 获取用户所在部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> getUserDeptIds(long userId);

    /**
     * 获取用户所在部门的所有子部门
     *
     * @param userId      用户ID
     * @param includeSelf 是否包含当前部门
     * @return 部门列表
     */
    List<TenantDept> getUserDescendant(long userId, boolean includeSelf);

    /**
     * 获取用户绑定的所有角色
     *
     * @param userId 用户ID
     * @return {@link RoleType}
     */
    List<RoleType> getUserRoles(long userId);

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
