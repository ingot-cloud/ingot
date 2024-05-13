package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnums;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.model.BaseModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseModel<SysMenu> {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotNull(message = "{Common.IDNonNull}", groups = {Group.Update.class, Group.Delete.class})
    private Long id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单类型
     */
    private MenuTypeEnums menuType;

    /**
     * 菜单url
     */
    @NotBlank(message = "{SysMenu.path}", groups = Group.Create.class)
    private String path;

    /**
     * 权限ID
     */
    private Long authorityId;

    /**
     * 是否自定义视图路径
     */
    private Boolean customViewPath;

    /**
     * 视图路径
     */
    private String viewPath;

    /**
     * 命名路由
     */
    private String routeName;

    /**
     * 重定向
     */
    private String redirect;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否缓存
     */
    private Boolean isCache;

    /**
     * 是否隐藏
     */
    private Boolean hidden;

    /**
     * 是否隐藏面包屑
     */
    private Boolean hideBreadcrumb;

    /**
     * 是否匹配props
     */
    private Boolean props;

    /**
     * 菜单组织类型
     */
    private OrgTypeEnums orgType;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 创建日期
     */
    private LocalDateTime createdAt;

    /**
     * 更新日期
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;

}
