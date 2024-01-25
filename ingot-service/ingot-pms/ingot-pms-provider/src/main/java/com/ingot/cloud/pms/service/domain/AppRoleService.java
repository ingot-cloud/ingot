package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.dto.role.RoleFilterDTO;
import com.ingot.cloud.pms.api.model.vo.role.RoleGroupItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.framework.core.model.support.Option;
import com.ingot.framework.data.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
public interface AppRoleService extends BaseService<AppRole> {
    /**
     * 获取用户所有可用角色，包括用户基本角色和部门角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<AppRole> getRolesOfUser(long userId);

    /**
     * 根据角色编码获取角色
     *
     * @param code 角色编码
     * @return {@link AppRole}
     */
    AppRole getRoleByCode(String code);

    /**
     * 获取可用的options
     *
     * @return {@link List<Option>}
     */
    List<Option<Long>> options(boolean isAdmin);

    /**
     * 条件查询角色
     *
     * @param condition 筛选条件
     * @param isAdmin   是否为超级管理员
     * @return {@link List}，数据项结构 {@link RolePageItemVO}
     */
    List<RolePageItemVO> conditionList(AppRole condition, boolean isAdmin);

    /**
     * 角色组列表，包含子角色
     *
     * @param isAdmin 是否为管理员
     * @param filter  {@link RoleFilterDTO}
     * @return {@link RoleGroupItemVO}
     */
    List<RoleGroupItemVO> groupRoleList(boolean isAdmin, RoleFilterDTO filter);

    /**
     * 创建角色
     *
     * @param params  创建参数
     * @param isAdmin 是否为超级管理员
     */
    void createRole(AppRole params, boolean isAdmin);

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
    void updateRoleById(AppRole params, boolean isAdmin);

    /**
     * 排序角色组
     *
     * @param list 组id列表
     */
    void sortGroup(List<Long> list);

    /**
     * 创建角色组
     *
     * @param params  {@link AppRoleGroup}
     * @param isAdmin 是否为管理员
     */
    void createGroup(AppRoleGroup params, boolean isAdmin);

    /**
     * 更新角色组
     *
     * @param params  {@link AppRoleGroup}
     * @param isAdmin 是否为管理员
     */
    void updateGroup(AppRoleGroup params, boolean isAdmin);

    /**
     * 删除角色组
     *
     * @param id      ID
     * @param isAdmin 是否为管理员
     */
    void deleteGroup(long id, boolean isAdmin);
}
