package com.ingot.framework.base.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : IdDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 上午11:12.</p>
 */
@ApiModel(value = "IdDto")
@Data
public class IdDto implements Serializable {
    @ApiModelProperty(value = "id", required = true)
    private String id;
}
