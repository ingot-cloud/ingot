package com.ingot.framework.commons.model.iam;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>分页返回当前允许的部门分支及面包屑，不包含隐藏祖先名称。</p>
 * @author jy
 * @since 1.0.0
 * @param items 当前分支部门
 * @param total 当前分支可见部门数
 * @param page 从1开始的页号
 * @param pageSize 每页条数
 * @param breadcrumbs 当前可见路径
 * @param organization 可见的根组织名称，未授权时省略
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@Schema(description = "组织成员选择器的部门分支分页")
public record DepartmentNavigationPage(List<DepartmentNavigationEntry> items, long total, int page, int pageSize,
                                       List<DepartmentNavigationEntry> breadcrumbs, DepartmentNavigationEntry organization) { }
