package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.*;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.framework.data.mybatis.service.BaseService;
import com.ingot.framework.security.core.userdetails.IngotUser;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysUserService extends BaseService<SysUser> {

    /**
     * 通过 {@link IngotUser} 获取用户信息
     *
     * @param user {@link IngotUser} 当前登录用户
     * @return {@link UserInfoDTO}
     */
    UserInfoDTO getUserInfo(IngotUser user);

    /**
     * 获取平台超级管理员ID列表
     *
     * @return id列表
     */
    List<Long> getAdminIdList();

    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @param orgId     组织ID
     * @return {@link IPage}，数据项结构 {@link UserPageItemVO}
     */
    IPage<UserPageItemVO> conditionPage(Page<SysUser> page, UserQueryDTO condition, Long orgId);

    /**
     * 条件查询所有系统用户分页数据
     *
     * @param page   分页参数
     * @param filter 过滤条件{@link AllOrgUserFilterDTO}
     * @return {@link SysUser}
     */
    IPage<SysUser> allOrgUserPage(Page<SysUser> page, AllOrgUserFilterDTO filter);

    /**
     * 创建用户
     *
     * @param params 参数
     * @return {@link SysUser}
     */
    SysUser createUserAndSetRelation(UserDTO params);

    /**
     * 创建用户
     *
     * @param user {@link SysUser}
     */
    void createUser(SysUser user);

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     */
    void removeUserById(long id);

    /**
     * 更新用户信息
     *
     * @param params 参数
     */
    void updateUserAndUpdateRelation(UserDTO params);

    /**
     * 更新用户
     *
     * @param user {@link SysUser}
     */
    void updateUser(SysUser user);

    /**
     * 用户修改密码
     *
     * @param id     用户ID
     * @param params 参数
     */
    void fixPassword(long id, UserPasswordDTO params);

}
