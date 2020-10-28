package com.ingot.cloud.pms.api.model.dto.client;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : AppDeleteParams.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/19.</p>
 * <p>Time         : 11:03 AM.</p>
 */
@Data
public class ClientDeleteParams implements Serializable {
    @NotBlank(message = "应用id不能为空")
    private String client_id;
}
