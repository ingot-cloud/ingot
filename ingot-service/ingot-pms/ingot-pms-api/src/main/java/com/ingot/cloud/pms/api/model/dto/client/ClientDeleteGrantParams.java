package com.ingot.cloud.pms.api.model.dto.client;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : AppDeleteGrantParams.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/20.</p>
 * <p>Time         : 2:11 PM.</p>
 */
@Data
public class ClientDeleteGrantParams implements Serializable {
    @NotBlank(message = "应用id不能为空")
    private String client_id;
    private List<String> delete_list;
}
