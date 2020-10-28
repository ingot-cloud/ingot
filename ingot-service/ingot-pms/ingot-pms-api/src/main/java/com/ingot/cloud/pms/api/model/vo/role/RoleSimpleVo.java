package com.ingot.cloud.pms.api.model.vo.role;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : RoleSimpleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/4.</p>
 * <p>Time         : 4:37 PM.</p>
 */
@Data
public class RoleSimpleVo implements Serializable {
    /**
     * Id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private String id;
    /**
     * 角色编码
     */
    private String role_code;

    /**
     * 角色名称
     */
    private String role_name;
}
