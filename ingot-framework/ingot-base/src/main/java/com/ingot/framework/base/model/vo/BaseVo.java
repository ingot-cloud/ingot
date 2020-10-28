package com.ingot.framework.base.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>Description  : BaseVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/27.</p>
 * <p>Time         : 上午10:32.</p>
 */
@Data
public class BaseVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creator_id;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date created_time;

    /**
     * 最后操作人
     */
    private String last_operator;

    /**
     * 最后操作人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long last_operator_id;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;

}
