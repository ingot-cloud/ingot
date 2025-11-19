package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.framework.commons.model.common.RelationDTO;
import com.ingot.framework.commons.model.support.Option;

/**
 * <p>Description  : 业务角色处理，包含元数据.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 09:33.</p>
 */
public interface BizRoleService {

    /**
     * 获取用户绑定的所有角色
     *
     * @param userId 用户ID
     * @return {@link RoleType}
     */
    List<RoleType> getUserRoles(long userId);

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
     * @param roleId 角色ID
     * @return {@link BizAuthorityTreeNodeVO}
     */
    List<BizAuthorityTreeNodeVO> getRoleAuthoritiesTree(long roleId);

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
     * 绑定权限
     *
     * @param params {@link RelationDTO}
     */
    void bindAuthorities(RelationDTO<Long, Long> params);

    /**
     * 绑定用户
     *
     * @param params {@link RelationDTO}
     */
    void bindUsers(RelationDTO<Long, Long> params);
}
