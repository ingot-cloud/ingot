package com.ingot.cloud.pms.api.model.dto.authority;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : AuthorityCreateDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/7.</p>
 * <p>Time         : 9:19 AM.</p>
 */
@Data
public class AuthorityCreateDto implements Serializable {

    /**
     * api路径
     */
    @NotBlank(message = "url不能为空")
    private String url;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    private String authority_name;

    /**
     * 权限
     */
    @NotBlank(message = "url不能为空")
    private String authority_code;

    /**
     * 节点类型，0分支节点，1叶子节点
     */
    private String leaf;

    /**
     * 父节点id
     */
    private String pid;

    /**
     * 备注
     */
    private String remark;
}
