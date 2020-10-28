package com.ingot.cloud.pms.api.model.dto.client;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : AppCreateParams.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/19.</p>
 * <p>Time         : 9:31 AM.</p>
 */
@Data
public class ClientCreateParams implements Serializable {

    @NotBlank(message = "应用id不能为空")
    private String client_id;

    @NotBlank(message = "应用资源id不能为空")
    private String resource_id;

    private String resource_ids;

    private String scope;

    @NotBlank(message = "授权类型不能为空")
    private String authorized_grant_types;

    private String web_server_redirect_uri;

    private String authorities;

    private Integer access_token_validity;

    private Integer refresh_token_validity;

    private String additional_information;

    private String autoapprove;

    @NotBlank(message = "应用描述不能为空")
    private String description;

    private String auth_type;

    /**
     * client类型
     */
    @NotBlank(message = "应用类型不能为空")
    private String type;
}
