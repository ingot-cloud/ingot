package com.ingot.framework.commons.constants;

/**
 * <p>Description  : SocialConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:56 上午.</p>
 */
public interface SocialConstants {

    /**
     * QQ获取token
     */
    String QQ_AUTHORIZATION_CODE_URL = "https://graph.qq.com/oauth2.0/token?grant_type="
            + "authorization_code&code=%S&client_id=%s&redirect_uri=" + "%s&client_secret=%s";

    /**
     * 微信获取OPENID
     */
    String WX_AUTHORIZATION_CODE_URL = "https://api.weixin.qq.com/sns/oauth2/access_token"
            + "?appid=%s&secret=%s&code=%s&grant_type=authorization_code";

    /**
     * 微信小程序OPENID
     */
    String MINI_APP_AUTHORIZATION_CODE_URL = "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /**
     * 码云获取token
     */
    String GITEE_AUTHORIZATION_CODE_URL = "https://gitee.com/oauth/token?grant_type="
            + "authorization_code&code=%S&client_id=%s&redirect_uri=" + "%s&client_secret=%s";

    /**
     * 开源中国获取token
     */
    String OSC_AUTHORIZATION_CODE_URL = "https://www.oschina.net/action/openapi/token";

    /**
     * QQ获取用户信息
     */
    String QQ_USER_INFO_URL = "https://graph.qq.com/oauth2.0/me?access_token=%s";

    /**
     * 码云获取用户信息
     */
    String GITEE_USER_INFO_URL = "https://gitee.com/api/v5/user?access_token=%s";

    /**
     * 开源中国用户信息
     */
    String OSC_USER_INFO_URL = "https://www.oschina.net/action/openapi/user?access_token=%s&dataType=json";

    /**
     * 钉钉获取 token
     */
    String DING_OLD_GET_TOKEN = "https://oapi.dingtalk.com/gettoken";

    /**
     * 钉钉同步角色列表
     */
    String DING_OLD_ROLE_URL = "https://oapi.dingtalk.com/topapi/role/list";

    /**
     * 钉钉同步部门列表
     */
    String DING_OLD_DEPT_URL = "https://oapi.dingtalk.com/topapi/v2/department/listsub";

    /**
     * 钉钉部门用户id列表
     */
    String DING_DEPT_USERIDS_URL = "https://oapi.dingtalk.com/topapi/user/listid";

    /**
     * 钉钉用户详情
     */
    String DING_USER_INFO_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";
}
