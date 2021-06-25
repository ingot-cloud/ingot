package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

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
     * 创建用户角色关系，如果已存在则忽略
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("userId") long userId);

    /**
     * 获取角色绑定的用户
     *
     * @param page   分页参数
     * @param roleId 角色ID
     * @return 分页信息
     */
    IPage<SysUser> getRoleBindUsers(Page<?> page, @Param("roleId") long roleId);
}
