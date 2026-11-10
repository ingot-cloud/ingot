package com.ingot.cloud.iam.application.group.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * <p>列表和选择器使用的用户组元数据。</p>
 * @author jy
 * @since 1.0.0
 * @param id 组 ID
 * @param name 组名称
 * @param description 可空说明
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupSummary(String id, String name, String description) { }
