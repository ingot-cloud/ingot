package com.ingot.framework.core.constants;

/**
 * <p>Description  : BeanIds.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/26.</p>
 * <p>Time         : 5:50 PM.</p>
 */
public interface BeanIds {
    /**
     * kaptcha bean
     */
    String CAPTCHA = "captchaProducer";

    String JWT_ACCESS_TOKEN_CONVERTER = "jwtAccessTokenConverter";
    String TOKEN_ENHANCER = "jwtTokenEnhancer";

    String USER_AUTHENTICATION_SUCCESS_HANDLER = "ingotUserAuthenticationSuccessHandler";
    String USER_AUTHENTICATION_FAILURE_HANDLER = "ingotUserAuthenticationFailureHandler";

    String CLIENT_DETAIL_SERVICE = "ingotClientDetailService";
    String USER_DETAIL_SERVICE = "userDetailsService";

    String OAUTH2_REST_TEMPLATE = "ingotOAuth2RestTemplate";

    String CLIENT_DETAIL_PASSWORD_ENCODER = "clientDetailPasswordEncoder";
    String USER_PASSWORD_ENCODER = "userPasswordEncoder";

    String ACCESS_DENIED_HANDLER = "ingotAccessDeniedHandler";
    String AUTHENTICATION_ENTRY_POINT = "ingotAuthenticationEntryPoint";

    String CLIENT_TOKEN_UTILS = "clientTokenUtils";

    String TASK_EXECUTOR = "taskExecutor";

    String REDIS_TEMPLATE = "redisTemplate";

    /*
     * Validate Code Start
     */
    String VALIDATE_CODE_SECURITY_CONFIG = "validateCodeSecurityConfig";
    /*
     * Validate Code End
     */

    String RSA_SERVICE = "rsaService";
}
