package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.framework.store.mybatis.service.BaseService;

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
    List<DeptTreeNodeVO> tree();

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
    void removeDeptById(int id);

    /**
     * 更新部门
     *
     * @param params 参数
     */
    void updateDept(SysDept params);
}
