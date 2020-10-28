package com.ingot.cloud.pms.api.model.dto.authority;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : AuthorityUpdateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/7.</p>
 * <p>Time         : 9:22 AM.</p>
 */
@Data
public class AuthorityUpdateDto implements Serializable {

    /**
     * id
     */
    @NotBlank(message = "id不能为空")
    private String id;

    /**
     * api路径
     */
    private String url;

    /**
     * 权限名称
     */
    private String authority_name;

    /**
     * 权限
     */
    private String authority_code;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
