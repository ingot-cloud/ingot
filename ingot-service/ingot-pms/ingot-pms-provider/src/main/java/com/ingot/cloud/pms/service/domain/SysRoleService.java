package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.data.mybatis.common.service.BaseService;

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
     * 获取用户所有可用角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> getRolesOfUser(long userId);

    /**
     * 获取可用的options
     *
     * @return {@link List< Option >}
     */
    List<Option<Long>> options(boolean isAdmin);

    /**
     * 条件查询角色
     *
     * @param condition 筛选条件
     * @param isAdmin   是否为超级管理员
     * @return {@link List}，数据项结构 {@link RolePageItemVO}
     */
    List<RolePageItemVO> conditionList(SysRole condition, boolean isAdmin);

    /**
     * 角色组列表，包含子角色
     *
     * @param isAdmin 是否为管理员
     * @param filter  {@link RoleFilterDTO}
     * @return {@link RoleGroupItemVO}
     */
    List<RoleGroupItemVO> groupRoleList(boolean isAdmin, RoleFilterDTO filter);

    /**
     * 条件查询角色分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @param isAdmin   是否为超级管理员
     * @return {@link IPage}，数据项结构 {@link RolePageItemVO}
     */
    IPage<RolePageItemVO> conditionPage(Page<SysRole> page, SysRole condition, boolean isAdmin);

    /**
     * 根据角色编码获取角色
     *
     * @param code 角色编码
     * @return {@link SysRole}
     */
    SysRole getRoleByCode(String code);

    /**
     * 根据角色编码获取角色
     *
     * @param codes 角色编码列表
     * @return {@link SysRole}
     */
    List<SysRole> getRoleListByCodes(List<String> codes);

    /**
     * 创建角色
     *
     * @param params  创建参数
     * @param isAdmin 是否为超级管理员
     */
    void createRole(SysRole params, boolean isAdmin);

    /**
     * 根据ID删除角色
     *
     * @param id      角色ID
     * @param isAdmin 是否为超级管理员
     */
    void removeRoleById(long id, boolean isAdmin);

    /**
     * 根据ID更新角色
     *
     * @param params  更新参数
     * @param isAdmin 是否为超级管理员
     */
    void updateRoleById(SysRole params, boolean isAdmin);

    /**
     * 排序角色组
     *
     * @param list 组id列表
     */
    void sortGroup(List<Long> list);

    /**
     * 创建角色组
     *
     * @param params  {@link SysRoleGroup}
     * @param isAdmin 是否为管理员
     */
    void createGroup(SysRoleGroup params, boolean isAdmin);

    /**
     * 更新角色组
     *
     * @param params  {@link SysRoleGroup}
     * @param isAdmin 是否为管理员
     */
    void updateGroup(SysRoleGroup params, boolean isAdmin);

    /**
     * 删除角色组
     *
     * @param id      ID
     * @param isAdmin 是否为管理员
     */
    void deleteGroup(long id, boolean isAdmin);
}
