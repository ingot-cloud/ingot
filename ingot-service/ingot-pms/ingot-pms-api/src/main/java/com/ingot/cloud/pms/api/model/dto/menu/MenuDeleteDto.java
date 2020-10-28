package com.ingot.cloud.pms.api.model.dto.menu;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : MenuDeleteDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/7.</p>
 * <p>Time         : 10:04 AM.</p>
 */
@Data
public class MenuDeleteDto implements Serializable {
    @NotBlank(message = "id不能为空")
    private String id;
}
