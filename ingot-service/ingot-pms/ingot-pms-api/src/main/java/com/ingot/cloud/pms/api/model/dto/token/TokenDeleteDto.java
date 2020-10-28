package com.ingot.cloud.pms.api.model.dto.token;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : TokenDeleteDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/12.</p>
 * <p>Time         : 3:23 PM.</p>
 */
@Data
public class TokenDeleteDto implements Serializable {
    @NotBlank(message = "id不能为空")
    private String id;
}
