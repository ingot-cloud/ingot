//package com.ingot.framework.security.provider.authorize;
//
//import com.ingot.framework.security.config.SecurityConfigProvider;
//import com.ingot.framework.security.provider.filter.OAuth2ExceptionTranslationFilter;
//import com.ingot.framework.security.provider.filter.UserAuthenticationFilter;
//import com.ingot.framework.security.service.ResourcePermitService;
//import com.ingot.framework.security.service.TokenService;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.access.ExceptionTranslationFilter;
//
///**
// * <p>Description  : SecurityFilterConfigProvider.</p>
// * <p>Author       : wangchao.</p>
// * <p>Date         : 2019/6/5.</p>
// * <p>Time         : 10:53 AM.</p>
// */
//@Slf4j
//@RequiredArgsConstructor
//public class SecurityFilterConfigProvider implements SecurityConfigProvider {
//    private final ResourcePermitService resourcePermitService;
//    private final TokenService tokenService;
//
//    @Override public boolean config(HttpSecurity http) throws Exception {
//        log.info(">>> SecurityFilterConfigProvider - configure.");
//        UserAuthenticationFilter userAuthenticationFilter = new UserAuthenticationFilter(
//                resourcePermitService, tokenService);
//        http.addFilterAfter(userAuthenticationFilter, ExceptionTranslationFilter.class);
//
//        OAuth2ExceptionTranslationFilter translationFilter = new OAuth2ExceptionTranslationFilter();
//        http.addFilterBefore(translationFilter, ExceptionTranslationFilter.class);
//        return false;
//    }
//}
