package com.ingot.cloud.pms.api.model.vo.menu;

import com.ingot.framework.base.model.vo.TreeVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : MenuVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/6.</p>
 * <p>Time         : 9:12 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuVo extends TreeVo {

    /**
     * 父组织编码
     */
    private String parent_menu_code;

    /**
     * 父组织名称
     */
    private String parent_menu_name;

    /**
     * 菜单编码
     */
    private String menu_code;

    /**
     * 菜单名称
     */
    private String menu_name;

    /**
     * 状态
     */
    private String status;

    /**
     * 菜单URL
     */
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

    /**
     * child
     */
    private List<MenuVo> child;

    /**
     * 是否被禁用
     */
    private boolean disabled;

}
