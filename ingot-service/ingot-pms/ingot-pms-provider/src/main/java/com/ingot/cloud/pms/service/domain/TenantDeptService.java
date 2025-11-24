package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithMemberCountDTO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface TenantDeptService extends BaseService<TenantDept> {
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
    List<DeptTreeNodeVO> treeList(TenantDept condition);

    /**
     * 获取部门列表，并且返回部门成员数量
     *
     * @return {@link DeptWithMemberCountDTO}
     */
    List<DeptWithMemberCountDTO> listWithMemberCount();

    /**
     * 创建部门
     *
     * @param params 参数
     */
    void create(TenantDept params);

    /**
     * 更新部门
     *
     * @param params 参数
     */
    void update(TenantDept params);

    /**
     * 删除部门
     *
     * @param id 部门ID
     */
    void delete(long id);

    /**
     * 获取主要部门
     *
     * @return {@link TenantDept}
     */
    TenantDept getMainDept();

    /**
     * 更具租户ID清除数据
     *
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);
}
