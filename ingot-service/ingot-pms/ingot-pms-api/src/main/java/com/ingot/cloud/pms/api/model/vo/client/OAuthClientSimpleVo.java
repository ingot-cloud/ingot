package com.ingot.cloud.pms.api.model.vo.client;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : OAuthClientSimpleVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/4.</p>
 * <p>Time         : 3:47 PM.</p>
 */
@Data
public class OAuthClientSimpleVo implements Serializable {
    /**
     * Client ID
     */
    private String client_id;
    /**
     * 描述
     */
    private String description;
    /**
     * client类型
     */
    private String type;
}
