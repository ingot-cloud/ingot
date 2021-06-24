package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDto;
import com.ingot.cloud.pms.api.model.dto.user.UserDto;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDto;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDto;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVo;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.store.mybatis.service.BaseService;

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
     * @return {@link UserInfoDto}
     */
    UserInfoDto getUserInfo(IngotUser user);

    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @return {@link IPage}，数据项结构 {@link UserPageItemVo}
     */
    IPage<UserPageItemVo> conditionPage(Page<SysUser> page, UserDto condition);

    /**
     * 创建用户
     *
     * @param params 参数
     */
    void createUser(UserDto params);

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
    void updateUser(UserDto params);

    /**
     * 更新用户基本信息
     *
     * @param params 基本信息参数
     */
    void updateUserBaseInfo(long id, UserBaseInfoDto params);

    /**
     * 用户修改密码
     * @param id 用户ID
     * @param params 参数
     */
    void fixPassword(long id, UserPasswordDto params);
}
