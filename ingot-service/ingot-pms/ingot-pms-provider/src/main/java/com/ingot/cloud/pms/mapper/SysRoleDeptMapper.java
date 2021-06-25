package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
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
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {
    /**
     * 创建部门角色关系，如果已存在则忽略
     *
     * @param roleId 角色ID
     * @param deptId 部门ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("deptId") long deptId);

    /**
     * 获取角色绑定的部门信息
     *
     * @param page   分页信息
     * @param roleId 角色Id
     * @return 分页信息
     */
    IPage<SysDept> getRoleBindDepts(Page<?> page, @Param("roleId") long roleId);
}
