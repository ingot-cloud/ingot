package com.ingot.cloud.pms.mapper;

import com.ingot.cloud.pms.api.model.domain.SysRoleUserDept;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2025-03-08
 */
@Mapper
public interface SysRoleUserDeptMapper extends BaseMapper<SysRoleUserDept> {
    /**
     * 根据部门和角色获取角色用户关联ID
     *
     * @param deptId 部门ID
     * @param roleId 角色ID
     * @return {@link SysRoleUserDept#getId()}
     */
    List<Long> getRoleUserDeptIdsByDeptAndRole(@Param("deptId") long deptId,
                                               @Param("roleId") long roleId);
}
