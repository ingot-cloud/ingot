//package com.ingot.framework.security.provider.authorize;
//
//import java.util.List;
//
//import com.ingot.framework.security.config.SecurityConfigProvider;
//import com.ingot.framework.security.service.ResourcePermitService;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//
///**
// * <p>Description  : AuthorizePermitConfig.</p>
// * <p>Author       : wangchao.</p>
// * <p>Date         : 2018/9/19.</p>
// * <p>Time         : 上午9:46.</p>
// */
//@Slf4j
//@RequiredArgsConstructor
//public class SecurityPermitConfigProvider implements SecurityConfigProvider {
//    private final String applicationName;
//    private final ResourcePermitService resourcePermitService;
//
//    @Override public boolean config(HttpSecurity http) throws Exception{
//        List<String> permit = resourcePermitService.allResourcePermitAntPatterns();
//        log.info(">>> {} AuthorizePermitConfig [configure] ========>>> http permit: {}", applicationName, permit);
//
//        if (!permit.isEmpty()){
//            http.authorizeRequests().antMatchers(permit.toArray(new String[0])).permitAll();
//        }
//
//        return false;
//    }
//}