package com.ingot.framework.base.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : OperatorDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/26.</p>
 * <p>Time         : 下午2:40.</p>
 */
@Data
public class OperatorDto implements Serializable {
    /**
     * 操作人员 ID
     */
    private Long userId;

    /**
     * 操作人员账号
     */
    private String userName;
}
