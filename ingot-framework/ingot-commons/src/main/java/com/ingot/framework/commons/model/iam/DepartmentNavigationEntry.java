package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>组织人员选择器的只读部门导航，不能作为人员被选择。</p>
 * @author jy
 * @since 1.0.0
 * @param id 当前租户可见部门ID
 * @param name 可披露部门名称
 */
@Schema(description = "只能展开的部门导航")
public record DepartmentNavigationEntry(String id, String name) { }
