package com.ingot.cloud.iam.api.model.domain;

import java.io.Serial;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>菜单与具体权限的可见性关联，不表示菜单拥有该资源的全部操作。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@TableName("platform_menu_permission")
public class PlatformMenuPermission extends BaseModel<PlatformMenuPermission> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 菜单 ID
     */
    private Long menuId;

    /**
     * 具体权限 ID，必须为同应用启用的 ACTION
     */
    private Long permissionId;
}
