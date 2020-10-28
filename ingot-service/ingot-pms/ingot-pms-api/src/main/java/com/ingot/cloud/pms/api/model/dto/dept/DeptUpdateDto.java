package com.ingot.cloud.pms.api.model.dto.dept;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * <p>Description  : DeptUpdateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 11:10 AM.</p>
 */
@Data
public class DeptUpdateDto {
    @NotBlank(message = "id不能为空")
    private String id;

    /**
     * 父ID
     */
    private Long pid;

    /**
     * 名称
     */
    private String name;

    /**
     * 排序
     */
    private String sort;

    /**
     * 状态, enable  disable
     */
    private String status;
}
