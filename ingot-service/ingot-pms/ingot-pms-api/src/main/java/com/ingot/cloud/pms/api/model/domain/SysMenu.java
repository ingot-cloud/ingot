package com.ingot.cloud.pms.api.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ingot.framework.store.mybatis.model.BaseModel;
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
    @TableId
    private Long id;

    /**
     * 版本号
     */
    @JsonIgnore
    @Version
    private Long version;

    /**
     * 租户ID
     */
    @JsonIgnoreProperties(allowSetters = true)
    private Integer tenantId;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单url
     */
    private String path;

    /**
     * 视图路径
     */
    private String viewPath;

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
    private Boolean cache;

    /**
     * 是否隐藏
     */
    private Boolean hidden;

    /**
     * 参数
     */
    private String params;

    /**
     * 状态, 0:正常，9:禁用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

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
