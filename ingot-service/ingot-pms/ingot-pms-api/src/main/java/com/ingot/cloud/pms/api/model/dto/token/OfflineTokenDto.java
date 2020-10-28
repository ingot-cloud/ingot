package com.ingot.cloud.pms.api.model.dto.token;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : OfflineTokenDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/26.</p>
 * <p>Time         : 2:20 PM.</p>
 */
@Data
public class OfflineTokenDto implements Serializable {
    // 需要离线的 token
    @NotBlank(message = "token不能为空")
    private String token;
}
