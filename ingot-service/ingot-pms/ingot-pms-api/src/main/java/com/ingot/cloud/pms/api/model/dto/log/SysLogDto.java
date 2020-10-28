package com.ingot.cloud.pms.api.model.dto.log;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>Description  : SysLogDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-28.</p>
 * <p>Time         : 14:32.</p>
 */
@Data
public class SysLogDto implements Serializable {
    private Long id;

    /**
     * 所属租户
     */
    private Long tenantId;

    /**
     * 标题
     */
    private String title;

    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * IP地址
     */
    private String remoteAddr;

    /**
     * 请求地址
     */
    private String requestUrl;

    /**
     * 请求method
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseData;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 耗时,毫秒
     */
    private Long executeTime;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建时间
     */
    private Date createdTime;
}
