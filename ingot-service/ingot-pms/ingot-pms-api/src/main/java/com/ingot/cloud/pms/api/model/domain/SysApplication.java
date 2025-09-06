package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author jymot
 * @since 2023-11-23
 */
@Getter
@Setter
@TableName("sys_application")
public class SysApplication extends BaseModel<SysApplication> {

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
    private Long authorityId;

    /**
     * 是否为默认应用
     */
    private Boolean defaultApp;

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
}
