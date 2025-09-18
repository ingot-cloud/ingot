package com.ingot.cloud.pms.api.model.dto.org;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>Description  : CreateOrgDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 10:58 AM.</p>
 */
@Data
public class CreateOrgDTO implements Serializable {
    /**
     * 头像
     */
    private String avatar;
    /**
     * 租户名称
     */
    private String name;
    /**
     * 用户手机号
     */
    private String phone;
}
