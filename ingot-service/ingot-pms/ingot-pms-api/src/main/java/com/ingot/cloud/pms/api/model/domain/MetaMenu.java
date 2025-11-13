package com.ingot.cloud.pms.api.model.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.cloud.pms.api.model.enums.MenuLinkTypeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Getter
@Setter
@ToString
@TableName("meta_menu")
public class MetaMenu extends BaseModel<MetaMenu> {

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
    @TableField("`name`")
    private String name;

    /**
     * 菜单类型
     */
    private MenuTypeEnum menuType;

    /**
     * 菜单url
     */
    @TableField("`path`")
    private String path;

    /**
     * 是否开启权限
     */
    private Boolean enableAuthority;

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
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 链接类型
     */
    private MenuLinkTypeEnum linkType;

    /**
     * 链接url
     */
    private String linkUrl;

    /**
     * 状态, 0:正常，9:禁用
     */
    @TableField("`status`")
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
