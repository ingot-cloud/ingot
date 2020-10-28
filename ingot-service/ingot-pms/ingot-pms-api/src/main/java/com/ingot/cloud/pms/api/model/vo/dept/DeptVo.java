package com.ingot.cloud.pms.api.model.vo.dept;

import com.ingot.framework.base.model.vo.TreeVo;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : DeptVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 10:09 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
public class DeptVo extends TreeVo {

    /**
     * 组织名称
     */
    private String name;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否已删除
     */
    private boolean is_deleted;

    /**
     * 父组织名称
     */
    private String parent_name;

    /**
     * child
     */
    private List<DeptVo> child;

    /**
     * 是否被禁用
     */
    private boolean disabled;

    /**
     * 排序
     */
    private int sort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isIs_deleted() {
        return is_deleted;
    }

    public void setIs_deleted(boolean is_deleted) {
        this.is_deleted = is_deleted;
    }

    public String getParent_name() {
        return parent_name;
    }

    public void setParent_name(String parent_name) {
        this.parent_name = parent_name;
    }

    public List<DeptVo> getChild() {
        return child;
    }

    public void setChild(List<DeptVo> child) {
        this.child = child;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}
