package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
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
     * @return {@link UserInfoDTO}
     */
    UserInfoDTO getUserInfo(IngotUser user);

    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @return {@link IPage}，数据项结构 {@link UserPageItemVO}
     */
    IPage<UserPageItemVO> conditionPage(Page<SysUser> page, UserDTO condition);

    /**
     * 创建用户
     *
     * @param params 参数
     * @return {@link SysUser}
     */
    SysUser createUser(UserDTO params);

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
    void updateUser(UserDTO params);

    /**
     * 用户修改密码
     *
     * @param id     用户ID
     * @param params 参数
     */
    void fixPassword(long id, UserPasswordDTO params);

    /**
     * 是否有用户关联了指定部门
     *
     * @param deptId 部门ID
     * @return Boolean 是否关联
     */
    boolean matchDept(long deptId);

    /**
     * 是否有用户关联了指定部门中的任意一个
     *
     * @param deptIds 部门ID列表
     * @return 是否存在，只要有用户关联任一部门即返回ture
     */
    boolean anyMatchDept(List<Long> deptIds);
}
