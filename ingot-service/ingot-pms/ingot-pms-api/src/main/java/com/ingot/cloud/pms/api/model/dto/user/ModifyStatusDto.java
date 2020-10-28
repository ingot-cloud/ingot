package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : ModifyStatusDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 下午3:00.</p>
 */
@Data
public class ModifyStatusDto implements Serializable {

    @NotEmpty(message = "id不能为空")
    private String id;

    @NotEmpty(message = "状态不能为空")
    private String status;
}
