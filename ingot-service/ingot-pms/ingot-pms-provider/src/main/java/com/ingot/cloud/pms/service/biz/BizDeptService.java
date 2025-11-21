package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithManagerDTO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptWithManagerVO;
import com.ingot.framework.commons.constants.IDConstants;

/**
 * <p>Description  : BizDeptService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/18.</p>
 * <p>Time         : 4:26 PM.</p>
 */
public interface BizDeptService {

    /**
     * 获取部门的所有后代部门列表
     *
     * @param deptId      部门ID
     * @param includeSelf 是否包含当前部门
     * @return 部门列表
     */
    List<TenantDept> getDescendantList(Long deptId, boolean includeSelf);

    /**
     * 获取部门列表，并且返回每个部门的管理员信息
     *
     * @return {@link DeptWithManagerVO}
     */
    List<DeptWithManagerVO> listWithManager();

    /**
     * 组织部门列表, 主部门（组织）和一级部门平级<br>
     * 比如组织部门名称为：测试部门<br>
     * 测试部门下面有两个部门：A部门和B部门<br>
     * 那么列表返回：测试部门，A部门，B部门
     *
     * @return {@link DeptTreeNodeVO}
     */
    List<DeptTreeNodeVO> orgList();

    /**
     * 组织部门树，一级部门在主部门之下
     *
     * @return {@link DeptTreeNodeVO}
     */
    List<DeptTreeNodeVO> orgTree();

    /**
     * 设置部门主管
     *
     * @param deptId  部门ID
     * @param userIds 用户ID列表
     */
    void setDeptManager(long deptId, List<Long> userIds);

    /**
     * 创建部门，不可创建主部门(pid={@link IDConstants#ROOT_TREE_ID})
     *
     * @param params {@link TenantDept}
     */
    void orgCreateDept(DeptWithManagerDTO params);

    /**
     * 更新部门，不可更新主部门
     *
     * @param params {@link TenantDept}
     */
    void orgUpdateDept(DeptWithManagerDTO params);

    /**
     * 删除部门
     *
     * @param id 删除部门
     */
    void orgDeleteDept(long id);

    /**
     * 设置用户部门, 确保包含主要部门
     *
     * @param userId  用户ID
     * @param deptIds 待设置的部门，非主要部门
     */
    void setUserDeptsEnsureMainDept(long userId, List<Long> deptIds);
}
