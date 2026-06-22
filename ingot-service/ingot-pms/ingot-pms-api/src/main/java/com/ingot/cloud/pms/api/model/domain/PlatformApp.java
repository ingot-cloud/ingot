package com.ingot.cloud.pms.api.model.domain;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>平台应用实体，作为权限命名空间与菜单的归属根。</p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Getter
@Setter
@ToString
@TableName("platform_app")
public class PlatformApp extends BaseModel<PlatformApp> {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 应用编码，权限命名空间
     */
    private String code;

    /**
     * 应用类型
     */
    private OrgTypeEnum appType;

    /**
     * 全局排序
     */
    private Integer sort;

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
