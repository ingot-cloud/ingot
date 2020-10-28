package com.ingot.cloud.pms.api.model.vo.authority;

import com.ingot.framework.base.model.vo.TreeVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/5.</p>
 * <p>Time         : 5:38 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthorityVo extends TreeVo {

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
