package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import com.ingot.framework.store.mybatis.service.BaseService;

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
    void deptBindRoles(RelationDTO<Integer, Integer> params);

    /**
     * 角色绑定部门
     *
     * @param params 关联参数
     */
    void roleBindDepts(RelationDTO<Integer, Integer> params);

    /**
     * 获取角色部门信息
     *
     * @param roleId    角色ID
     * @param isBind    是否绑定
     * @param condition 条件
     * @return 分页信息
     */
    List<DeptTreeNodeVO> getRoleDepts(int roleId,
                                      boolean isBind,
                                      SysDept condition);
}
