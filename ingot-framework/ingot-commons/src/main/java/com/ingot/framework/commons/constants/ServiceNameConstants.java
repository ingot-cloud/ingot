package com.ingot.framework.commons.constants;

/**
 * <p>集中声明内部服务发现名称，供 RPC 客户端绑定目标服务。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public interface ServiceNameConstants {

    /**
     * IAM 身份与访问管理服务
     */
    String IAM_SERVICE = "in-service-iam";

    /**
     * 会员系统
     */
    String MEMBER_SERVICE = "in-service-member";

    /**
     * 安全中心服务
     */
    String SECURITY_SERVICE = "in-service-security";

    /**
     * 授权服务
     */
    String AUTH_SERVICE = "in-service-auth";

}
