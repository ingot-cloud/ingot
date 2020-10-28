package com.ingot.cloud.pms.api.model.vo.token;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ingot.cloud.pms.api.model.vo.client.OAuthClientSimpleVo;
import com.ingot.framework.base.model.vo.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>Description  : UserTokenVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 2:41 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserTokenVo extends BaseVo {

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long user_id;

    /**
     * 登录名
     */
    private String username;

    /**
     * 姓名
     */
    private String real_name;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 登陆IP地址
     */
    private String login_ip;

    /**
     * 登录地址
     */
    private String login_location;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date login_time;

    /**
     * 访问token
     */
    private String access_token;

    /**
     * 刷新token
     */
    private String refresh_token;

    /**
     * token类型
     */
    private String token_type;

    /**
     * 访问token的生效时间(秒)
     */
    private Integer access_token_validity;

    /**
     * 刷新token的生效时间(秒)
     */
    private Integer refresh_token_validity;

    /**
     * 0 在线 10已刷新 20 离线
     */
    private Integer status;

    /**
     * 组织ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long group_id;

    /**
     * 组织名称
     */
    private String group_name;

    /**
     * OAuth ClientId
     */
    private String client_id;

    /**
     * Client
     */
    private OAuthClientSimpleVo client;
}
