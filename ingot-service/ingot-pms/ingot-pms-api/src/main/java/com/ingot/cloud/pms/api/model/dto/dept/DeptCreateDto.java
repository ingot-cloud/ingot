package com.ingot.cloud.pms.api.model.dto.dept;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : DeptCreateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/8.</p>
 * <p>Time         : 11:08 AM.</p>
 */
@Data
public class DeptCreateDto implements Serializable {
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
}
