package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.cloud.pms.api.model.dto.token.TokenDto;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : HandleLoginDataDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/20.</p>
 * <p>Time         : 下午5:40.</p>
 */
@Data
public class HandleUserLoginDataDto implements Serializable {
    /**
     * 用户 token
     */
    private TokenDto token;
    /**
     * 用户信息
     */
    private HandleLoginUserInfoDto principal;
    /**
     * 客户端操作系统
     */
    private String os;
    /**
     * 获取客户端浏览器
     */
    private String browser;
    /**
     * 访问者IP
     */
    private String remoteIP;
    /**
     * 请求路径
     */
    private String requestURI;
    /**
     * 远程地址
     */
    private String remoteLocation;
}
