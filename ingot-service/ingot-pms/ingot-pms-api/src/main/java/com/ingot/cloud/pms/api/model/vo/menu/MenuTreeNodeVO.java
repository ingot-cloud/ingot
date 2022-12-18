package com.ingot.cloud.pms.api.model.vo.menu;

import com.ingot.cloud.pms.api.model.base.TreeNode;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : MenuTreeNodeVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/29.</p>
 * <p>Time         : 8:31 上午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuTreeNodeVO extends TreeNode<Long> {
    /**
     * 菜单名称
     */
    private String name;
    /**
     * 菜单编码
     */
    private String code;
    /**
     * 菜单url
     */
    private String path;
    /**
     * 视图路径
     */
    private String viewPath;
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
    private int sort;
    /**
     * 是否缓存
     */
    private boolean cache;
    /**
     * 是否隐藏
     */
    private boolean hidden;
    /**
     * 参数
     */
    private String params;
    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
    /**
     * 备注
     */
    private String remark;
}
