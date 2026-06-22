package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.dto.application.AppMenuCreateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppMenuUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppPermissionUpdateDTO;
import com.ingot.cloud.pms.api.model.dto.application.AppUpdateDTO;
import com.ingot.cloud.pms.api.model.vo.application.AppDetailVO;
import com.ingot.cloud.pms.api.model.vo.application.AppPermissionTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * <p>应用中心化资源对象转换器，覆盖详情/树节点映射与按非空字段的部分更新。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface ApplicationConvert {

    /**
     * 应用实体转详情视图（根权限编码、统计数量由调用方补充）。
     */
    @Mapping(target = "rootPermissionId", source = "permissionId")
    AppDetailVO toDetail(PlatformApp source);

    /**
     * 创建菜单 DTO 转菜单实体（默认值由调用方补充）。
     */
    PlatformMenu toMenu(AppMenuCreateDTO source);

    /**
     * 权限实体转应用权限树节点（{@code readOnly} 由调用方补充）。
     */
    AppPermissionTreeNodeVO toPermissionTreeNode(PlatformPermission source);

    /**
     * 应用更新：仅覆盖请求中显式传入的非空字段。
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateApp(AppUpdateDTO source, @MappingTarget PlatformApp target);

    /**
     * 菜单更新：仅覆盖请求中显式传入的非空字段。
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateMenu(AppMenuUpdateDTO source, @MappingTarget PlatformMenu target);

    /**
     * 权限更新：仅覆盖请求中显式传入的非空字段。
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePermission(AppPermissionUpdateDTO source, @MappingTarget PlatformPermission target);
}
