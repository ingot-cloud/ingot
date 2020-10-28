package com.ingot.cloud.pms.api.model.vo.tenant;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : TenantSimpleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/9.</p>
 * <p>Time         : 3:12 PM.</p>
 */
@Data
public class TenantSimpleVo implements Serializable {

    /**
     * Id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private String id;

    /**
     * 租户名称
     */
    private String name;

    /**
     * 租户编号
     */
    private String code;
}
