package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.data.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleDeptService extends BaseService<SysRoleDept> {
    /**
     * 部门绑定角色
     *
     * @param params 关联参数
     */
    void deptBindRoles(RelationDTO<Long, Long> params);

    /**
     * 角色绑定部门
     *
     * @param params 关联参数
     */
    void roleBindDepts(RelationDTO<Long, Long> params);

    /**
     * 根据角色获取部门
     *
     * @param roleId 角色ID
     * @return {@link SysDept} List
     */
    List<SysDept> getDeptsByRole(long roleId);

    /**
     * 获取角色部门信息
     *
     * @param roleId    角色ID
     * @param condition 条件
     * @return 分页信息
     */
    List<DeptTreeNodeVO> getRoleDepts(long roleId,
                                      SysDept condition);
}
