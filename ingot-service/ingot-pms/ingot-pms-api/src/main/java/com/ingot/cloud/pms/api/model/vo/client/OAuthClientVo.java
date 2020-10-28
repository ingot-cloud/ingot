package com.ingot.cloud.pms.api.model.vo.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.base.model.dto.OperatorDto;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>Description  : OAuthClientDetailsVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/16.</p>
 * <p>Time         : 9:46 AM.</p>
 */
@Data
public class OAuthClientVo implements Serializable {

    /**
     * Client ID
     */
    private String client_id;

    /**
     * Client 秘钥
     */
    private String client_secret;

    /**
     * 资源ID
     */
    private String resource_id;

    /**
     * 授权的资源ID
     */
    private String resource_ids;

    /**
     * 域
     */
    private String scope;

    /**
     * 可使用的授权模式
     */
    private String authorized_grant_types;

    /**
     * 重定向URL
     */
    private String web_server_redirect_uri;

    /**
     * 权限
     */
    private String authorities;

    /**
     * access token 有效时间
     */
    private Integer access_token_validity;

    /**
     * refresh token 有效时间
     */
    private Integer refresh_token_validity;

    /**
     * 额外参数
     */
    private String additional_information;

    /**
     * 跳过授权
     */
    private String autoapprove;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否已删除
     */
    private String is_deleted;

    /**
     * 是否已禁用
     */
    private String is_disabled;

    /**
     * 授权类型，默认standard
     */
    private String auth_type;

    /**
     * client类型
     */
    private String type;
    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建人ID
     */
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
    private Long last_operator_id;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;

    /**
     * 设置操作人员信息
     */
    @JsonIgnore
    public void setCreaterInfo(OperatorDto operator) {
        if (operator == null){
            return;
        }
        this.creator_id = operator.getUserId();
        this.creator = operator.getUserName();
        this.created_time = new Date();
        setLastOperatorInfo(operator);
    }

    /**
     * 设置操作人员信息
     */
    @JsonIgnore
    public void setLastOperatorInfo(OperatorDto operator){
        if (operator == null){
            return;
        }
        this.last_operator_id = operator.getUserId();
        this.last_operator = operator.getUserName();
        this.update_time = new Date();
    }
}
