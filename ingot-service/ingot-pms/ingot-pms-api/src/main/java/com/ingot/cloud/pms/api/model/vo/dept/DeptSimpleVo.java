package com.ingot.cloud.pms.api.model.vo.dept;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : DeptSimpleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/9.</p>
 * <p>Time         : 3:09 PM.</p>
 */
@Data
public class DeptSimpleVo implements Serializable {

    /**
     * Id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private String id;
    /**
     * 部门名称
     */
    private String name;
}
