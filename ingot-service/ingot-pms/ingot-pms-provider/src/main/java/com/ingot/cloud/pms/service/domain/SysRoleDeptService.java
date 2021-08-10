package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.framework.core.model.dto.common.RelationDto;
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
    void deptBindRoles(RelationDto<Long, Long> params);

    /**
     * 角色绑定部门
     *
     * @param params 关联参数
     */
    void roleBindDepts(RelationDto<Long, Long> params);

    /**
     * 获取角色部门信息
     *
     * @param roleId    角色ID
     * @param page      分页信息
     * @param isBind    是否绑定
     * @param condition 条件
     * @return 分页信息
     */
    IPage<SysDept> getRoleDepts(long roleId,
                                Page<?> page,
                                boolean isBind,
                                SysDept condition);
}
