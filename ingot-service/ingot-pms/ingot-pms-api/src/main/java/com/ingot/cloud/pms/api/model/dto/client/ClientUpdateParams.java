package com.ingot.cloud.pms.api.model.dto.client;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : AppUpdateParams.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/19.</p>
 * <p>Time         : 11:01 AM.</p>
 */
@Data
public class ClientUpdateParams implements Serializable {
    @NotBlank(message = "应用id不能为空")
    private String client_id;

    private String resource_ids;

    private String scope;

    private String authorized_grant_types;

    private String web_server_redirect_uri;

    private String authorities;

    private Integer access_token_validity;

    private Integer refresh_token_validity;

    private String additional_information;

    private String autoapprove;

    private String description;

    private String is_disabled;

    private String auth_type;

    /**
     * client类型
     */
    private String type;

}
