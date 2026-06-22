package com.ingot.cloud.pms.authorization.resource;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.dto.application.AppCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppMenuCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppMenuUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppPermissionCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppPermissionUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppStatusPatchDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppUpdateDTO;
import com.ingot.cloud.pms.api.model.vo.application.AppDetailVO;
import com.ingot.cloud.pms.api.model.vo.application.AppPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;

/**
 * <p>应用中心化资源领域服务，统一管理应用、菜单与权限的查询与写入。</p>
 *
 * <p>写入操作保证应用归属校验、编码规则与事务一致性，托管 NAVIGATION 权限随菜单生命周期维护。</p>
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
     * 创建应用菜单并托管 NAVIGATION 权限。
     * 目录类型菜单权限码追加 {@code :**}。
     */
    Long createMenu(long appId, AppMenuCreateDTO dto);

    /** 更新应用菜单并同步托管权限。 */
    void updateMenu(long appId, long menuId, AppMenuUpdateDTO dto);

    /** 删除叶子菜单及其托管权限。 */
    void deleteMenu(long appId, long menuId);

    /** 查询应用权限树。 */
    List<AppPermissionTreeNodeVO> getPermissionTree(long appId);

    /** 创建 GROUP / ACTION 权限。 */
    Long createPermission(long appId, AppPermissionCreateDTO dto);

    /** 更新非托管权限。 */
    void updatePermission(long appId, long permissionId, AppPermissionUpdateDTO dto);

    /** 删除非托管、无绑定的叶子权限。 */
    void deletePermission(long appId, long permissionId);
}
