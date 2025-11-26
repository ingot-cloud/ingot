package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.dto.role.BizRoleAssignUsersDTO;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.support.Option;

/**
 * <p>Description  : 业务角色处理，包含元数据.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 09:33.</p>
 */
public interface BizRoleService {

    /**
     * 获取元数据角色
     *
     * @param id 角色ID
     * @return {@link MetaRole}
     */
    MetaRole getMetaRole(long id);

    /**
     * 获取角色
     *
     * @param id 角色ID
     * @return {@link RoleType}
     */
    RoleType getRole(long id);

    /**
     * 获取角色
     *
     * @param ids 角色ID列表
     * @return {@link RoleType}
     */
    List<RoleType> getRoles(List<Long> ids);

    /**
     * 角色编码转换角色
     *
     * @param codes 角色编码列表
     * @return 角色列表
     */
    List<RoleType> getRolesByCodes(List<String> codes);

    /**
     * 根据编码获取角色
     *
     * @param code 角色编码
     * @return {@link RoleType}
     */
    RoleType getByCode(String code);

    /**
     * 角色下拉列表
     *
     * @param condition {@link TenantRolePrivate}
     * @return {@link Option}
     */
    List<Option<Long>> options(TenantRolePrivate condition);

    /**
     * 角色条件查询
     *
     * @param condition {@link TenantRolePrivate}
     * @return {@link RoleTreeNodeVO}
     */
    List<RoleTreeNodeVO> conditionTree(TenantRolePrivate condition);

    /**
     * 获取角色权限
     *
     * @param roleId 角色ID
     * @return {@link BizAuthorityVO}
     */
    List<BizAuthorityVO> getRoleAuthorities(long roleId);

    /**
     * 获取角色权限
     *
     * @param roleId    角色ID
     * @param condition 查询条件
     * @return {@link BizAuthorityTreeNodeVO}
     */
    List<BizAuthorityTreeNodeVO> getRoleAuthoritiesTree(long roleId, MetaAuthority condition);

    /**
     * 获取角色列表的权限
     *
     * @param roles 角色列表
     * @return 权限列表
     */
    List<AuthorityType> getRolesAuthorities(List<RoleType> roles);

    /**
     * 获取角色列表的权限及子权限
     *
     * @param roles 角色列表
     * @return 权限列表
     */
    List<AuthorityType> getRolesAuthoritiesAndChildren(List<RoleType> roles);

    /**
     * 创建角色
     *
     * @param params {@link TenantRolePrivate}
     */
    void create(TenantRolePrivate params);

    /**
     * 更新角色
     *
     * @param params {@link TenantRolePrivate}
     */
    void update(TenantRolePrivate params);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void delete(long id);

    /**
     * 排序
     */
    void sort(List<Long> ids);

    /**
     * 给角色设置权限，整体替换
     *
     * @param params {@link SetDTO}
     */
    void setAuthorities(SetDTO<Long, Long> params);

    /**
     * 角色分配用户
     *
     * @param params {@link BizRoleAssignUsersDTO}
     */
    void assignUsers(BizRoleAssignUsersDTO params);

    /**
     * 组织管理员分配权限
     *
     * @param ids    权限ID列表
     * @param assign 是否分配，true:分配，false:取消分配
     */
    void orgManagerAssignAuthorities(List<Long> ids, boolean assign);
}
