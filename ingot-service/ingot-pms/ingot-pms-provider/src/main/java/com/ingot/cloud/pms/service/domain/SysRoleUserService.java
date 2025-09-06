package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.vo.user.UserWithDeptVO;
import com.ingot.framework.commons.model.common.RelationDTO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleUserService extends BaseService<SysRoleUser> {

    /**
     * 根据用户ID删除相关关联角色
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean removeByUserId(long userId);


    /**
     * 更新用户角色，将用户角色更新为指定角色
     *
     * @param userId 用户ID
     * @param roles  待设置的角色
     */
    void setUserRoles(long userId, List<Long> roles);

    /**
     * 用户绑定角色
     *
     * @param params 关联参数
     */
    void userBindRoles(RelationDTO<Long, Long> params);

    /**
     * 角色绑定用户
     *
     * @param params 关联参数
     */
    void roleBindUsers(RelationDTO<Long, Long> params);

    /**
     * 获取角色用户
     *
     * @param roleId    角色id
     * @param page      分页信息
     * @param isBind    是否绑定
     * @param condition 查询条件
     * @return 分页用户
     */
    IPage<SysUser> getRoleUsers(long roleId,
                                Page<?> page,
                                boolean isBind,
                                SysUser condition);

    /**
     * 获取角色绑定的用户列表
     *
     * @param roleId 角色ID
     * @return {@link SysUser}
     */
    List<SysUser> getRoleUsers(long roleId);

    /**
     * 获取指定角色用户列表，并且过滤用户，只返回和部门关联角色相关的用户
     *
     * @param roleId 角色ID
     * @return {@link UserWithDeptVO}
     */
    List<UserWithDeptVO> getRoleUserWithDeptList(long roleId);

    /**
     * 获取指定角色列表所有人员
     *
     * @param roleIds 角色ID列表
     * @return {@link SysUser}
     */
    List<SysUser> getRoleListUsers(List<Long> roleIds);
}
