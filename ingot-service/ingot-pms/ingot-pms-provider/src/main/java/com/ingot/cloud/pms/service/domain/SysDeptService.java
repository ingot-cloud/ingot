package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysUserDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.framework.data.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysDeptService extends BaseService<SysDept> {

    /**
     * 获取部门tree
     *
     * @return 部门tree节点列表
     */
    List<DeptTreeNodeVO> treeList();

    /**
     * 条件tree
     *
     * @param condition 条件
     * @return tree列表
     */
    List<DeptTreeNodeVO> treeList(SysDept condition);

    /**
     * 创建部门
     *
     * @param params 参数
     */
    void createDept(SysDept params);

    /**
     * 删除部门
     *
     * @param id 部门ID
     */
    void removeDeptById(long id);

    /**
     * 更新部门
     *
     * @param params 参数
     */
    void updateDept(SysDept params);

    /**
     * 获取主要部门
     *
     * @return {@link SysDept}
     */
    SysDept getMainDept();

    /**
     * 根据用户ID和租户ID获取{@link SysUserDept}
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return {@link SysUserDept}
     */
    SysUserDept getByUserIdAndTenant(long userId, long tenantId);

    /**
     * 设置部门
     *
     * @param userId  用户ID
     * @param deptIds 部门ID列表
     */
    void setDepts(long userId, List<Long> deptIds);

    /**
     * 获取用户部门ID列表
     *
     * @param userId 用户ID
     * @return List
     */
    List<SysUserDept> getUserDepts(long userId);
}
