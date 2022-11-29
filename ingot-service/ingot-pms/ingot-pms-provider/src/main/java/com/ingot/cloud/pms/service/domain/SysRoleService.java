package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.framework.store.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleService extends BaseService<SysRole> {

    /**
     * 获取用户所有可用角色，包括用户基本角色和部门角色
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 角色列表
     */
    List<SysRole> getAllRolesOfUser(long userId, long deptId);

    /**
     * 根据clientId获取对应所有的角色
     *
     * @param clientIds 客户端ID列表
     * @return 角色列表
     */
    List<SysRole> getAllRolesOfClients(List<String> clientIds);

    /**
     * 获取用户所有可用角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> getRolesOfUser(long userId);

    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @return {@link IPage}，数据项结构 {@link RolePageItemVO}
     */
    IPage<RolePageItemVO> conditionPage(Page<SysRole> page, SysRole condition);

    /**
     * 创建角色
     *
     * @param params 创建参数
     */
    void createRole(SysRole params);

    /**
     * 根据ID删除角色
     *
     * @param id 角色ID
     */
    void removeRoleById(long id);

    /**
     * 根据ID更新角色
     *
     * @param params 更新参数
     */
    void updateRoleById(SysRole params);
}
