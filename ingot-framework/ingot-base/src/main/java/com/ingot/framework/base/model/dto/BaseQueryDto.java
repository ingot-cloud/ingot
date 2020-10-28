package com.ingot.framework.base.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : BaseQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/23.</p>
 * <p>Time         : 上午10:33.</p>
 */
@Data
public class BaseQueryDto implements Serializable {
    /**
     * 当前页
     */
    private Integer page_num = 1;

    /**
     * 每页条数
     */
    private Integer page_size = 10;

    /**
     * 排序
     */
    private String order_by;

    public String uniqueKey(){
        return "num" + page_num + "size" + page_size + "order" + order_by;
    }
}
