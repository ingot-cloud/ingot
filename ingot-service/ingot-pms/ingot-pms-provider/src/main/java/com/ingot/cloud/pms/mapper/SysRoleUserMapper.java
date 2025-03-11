package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.vo.user.UserWithDeptVO;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleUserMapper extends BaseMapper<SysRoleUser> {

    /**
     * 获取角色用户
     *
     * @param page   分页参数
     * @param roleId 角色ID
     * @param isBind 是否绑定
     * @return 分页信息
     */
    IPage<SysUser> getRoleUsers(Page<?> page,
                                @Param("roleId") long roleId,
                                @Param("isBind") boolean isBind,
                                @Param("condition") SysUser condition);

    /**
     * 获取角色绑定的用户列表
     *
     * @param roleId 角色ID
     * @return {@link SysUser}
     */
    List<SysUser> getRoleUserList(@Param("roleId") long roleId);

    /**
     * 获取指定角色用户列表，并且过滤用户，只返回和部门关联角色相关的用户
     *
     * @param roleId 角色ID
     * @return {@link UserWithDeptVO}
     */
    List<UserWithDeptVO> getRoleUserWithDeptList(@Param("roleId") long roleId);

    /**
     * 获取指定角色列表所有人员
     *
     * @param roleIds 角色ID列表
     * @return {@link SysUser}
     */
    List<SysUser> getRoleListUsers(@Param("roleIds") List<Long> roleIds);
}
