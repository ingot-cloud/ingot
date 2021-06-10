package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNode;
import com.ingot.framework.store.mybatis.service.BaseService;

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
    List<DeptTreeNode> tree();

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
}
