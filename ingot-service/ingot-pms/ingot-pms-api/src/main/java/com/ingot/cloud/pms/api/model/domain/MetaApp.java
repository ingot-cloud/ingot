package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
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
@TableName("meta_app")
public class MetaApp extends BaseModel<MetaApp> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 菜单ID
     */
    private Long menuId;

    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 应用名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 应用图标
     */
    private String icon;

    /**
     * 应用介绍
     */
    private String intro;

    /**
     * 状态
     */
    @TableField("`status`")
    private CommonStatusEnum status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除日期
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
