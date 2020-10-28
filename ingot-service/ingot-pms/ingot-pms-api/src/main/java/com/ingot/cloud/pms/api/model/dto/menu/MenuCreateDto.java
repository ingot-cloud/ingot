package com.ingot.cloud.pms.api.model.dto.menu;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : MenuCreateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/7.</p>
 * <p>Time         : 9:55 AM.</p>
 */
@Data
public class MenuCreateDto implements Serializable {

    /**
     * 父Id
     */
    private String pid;

    /**
     * 菜单编码
     */
    @NotBlank(message = "菜单编码不能为空")
    private String menu_code;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    private String menu_name;

    /**
     * 菜单URL
     */
    @NotBlank(message = "菜单url不能为空")
    private String url;

    /**
     * 图标
     */
    private String icon;

    /**
     * 视图路径
     */
    private String view;

    /**
     * 是否隐藏
     */
    private String hidden;

    /**
     * 0菜单，1按钮
     */
    private String type;

    /**
     * 0不缓存，1缓存
     */
    private String cache;

    /**
     * 参数
     */
    private String params;

    /**
     * 序号
     */
    private Integer number;

    /**
     * 备注
     */
    private String remark;

}
