package com.ingot.cloud.pms.authorization.resource;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.dto.application.AppCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppMenuCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppMenuUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppPermissionCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppPermissionUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppResourceCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppResourceUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppStatusPatchDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppUpdateDTO;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformResource;
import com.ingot.cloud.pms.api.model.vo.application.AppDetailVO;
import com.ingot.cloud.pms.api.model.vo.application.AppPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;

/**
 * <p>应用中心化资源领域服务，统一管理应用、菜单与权限的查询与写入。</p>
 *
 * <p>写入操作保证应用归属校验、编码规则与事务一致性。菜单不再托管权限生命周期。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface ApplicationResourceService {

    /** 分页查询应用。 */
    IPage<PlatformApp> pageApps(Page<PlatformApp> page, PlatformApp condition);

    /** 查询应用详情（含根权限与资源统计）。 */
    AppDetailVO getAppDetail(long appId);

    /**
     * 创建应用并自动生成 {@code appCode:**} 根权限。
     */
    Long createApp(AppCreateDTO dto);

    /** 更新应用基本信息（编码不可变）。 */
    void updateApp(long appId, AppUpdateDTO dto);

    /** 启用或禁用应用。 */
    void patchAppStatus(long appId, AppStatusPatchDTO dto);

    /**
     * 删除应用。
     *
     * <p>普通删除要求应用为空（无菜单、子权限、租户授权及角色绑定）；
     * {@code force=true} 为超级管理员强制删除，级联清除应用全部菜单、权限与平台角色绑定，
     * 但存在租户授权（{@code tenant_app_config}）时仍拒绝，以保护租户数据。</p>
     *
     * @param appId 应用ID
     * @param force 是否强制级联删除（仅超级管理员）
     */
    void deleteApp(long appId, boolean force);

    /** 查询应用内菜单树。 */
    List<MenuTreeNodeVO> getMenuTree(long appId);

    /**
     * 创建应用菜单。受保护页面必须关联已有具体权限，目录和 OPEN 不得关联。
     */
    Long createMenu(long appId, AppMenuCreateDTO dto);

    /** 更新应用菜单；路径变化不影响权限。 */
    void updateMenu(long appId, long menuId, AppMenuUpdateDTO dto);

    /** 删除叶子菜单及其可见性关联，不删除权限。 */
    void deleteMenu(long appId, long menuId);

    /** 查询应用权限树。 */
    List<AppPermissionTreeNodeVO> getPermissionTree(long appId);

    /** 创建 GROUP / ACTION 权限。 */
    Long createPermission(long appId, AppPermissionCreateDTO dto);

    /** 更新权限名称、备注与状态；不可改编码或资源绑定。 */
    void updatePermission(long appId, long permissionId, AppPermissionUpdateDTO dto);

    /** 删除非根、无引用的叶子权限。 */
    void deletePermission(long appId, long permissionId);

    /**
     * 列出应用资源目录。
     *
     * @param appId 应用 ID
     * @return 资源列表
     */
    List<PlatformResource> listResources(long appId);

    /**
     * 创建应用资源，编码在应用内唯一。
     *
     * @param appId 应用 ID
     * @param dto   创建参数
     * @return 资源 ID
     */
    Long createResource(long appId, AppResourceCreateDTO dto);

    /**
     * 更新资源名称或状态，不可改编码。
     *
     * @param appId      应用 ID
     * @param resourceId 资源 ID
     * @param dto        更新参数
     */
    void updateResource(long appId, long resourceId, AppResourceUpdateDTO dto);

    /**
     * 删除未被权限或数据规则引用的资源。
     *
     * @param appId      应用 ID
     * @param resourceId 资源 ID
     */
    void deleteResource(long appId, long resourceId);
}
