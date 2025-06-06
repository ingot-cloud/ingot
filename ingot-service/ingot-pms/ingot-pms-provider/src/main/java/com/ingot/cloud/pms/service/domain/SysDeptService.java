package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysUserDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithMemberCountDTO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
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
     * 获取部门列表，并且返回部门成员数量
     *
     * @return {@link DeptWithMemberCountDTO}
     */
    List<DeptWithMemberCountDTO> listWithMemberCount();

    /**
     * 获取用户所在部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> getUserDeptIds(long userId);

    /**
     * 获取用户所在部门的所有子部门
     *
     * @param userId      用户ID
     * @param includeSelf 是否包含当前部门
     * @return 部门列表
     */
    List<SysDept> getUserDescendant(long userId, boolean includeSelf);

    /**
     * 获取部门的所有后代部门列表
     *
     * @param deptId      部门ID
     * @param includeSelf 是否包含当前部门
     * @return 部门列表
     */
    List<SysDept> getDescendantList(Long deptId, boolean includeSelf);

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
