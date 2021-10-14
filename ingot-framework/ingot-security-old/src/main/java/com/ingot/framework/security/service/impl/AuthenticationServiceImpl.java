package com.ingot.framework.security.service.impl;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import com.ingot.framework.security.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : AuthenticationServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-30.</p>
 * <p>Time         : 14:07.</p>
 */
@Slf4j
@Service("ingotAuth")
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
//    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
//    @Resource
//    @Lazy
//    private  PmsRoleFeignApi ucRoleFeignApi;
//    @Value("${spring.application.name}")
//    private String serviceName;

    @Override public boolean authenticate(Authentication authentication, HttpServletRequest request) {
//        String requestUri = request.getRequestURI();
//        String from = request.getHeader(SecurityConstants.HEADER_FROM);
//        // 内部请求不鉴权
//        if (StrUtil.equals(from, SecurityConstants.HEADER_FROM_INSIDE_VALUE)){
//            log.info(">>> AuthenticationService - ignore authenticate url={}, request from {}.", requestUri, SecurityConstants.HEADER_FROM_INSIDE_VALUE);
//            return true;
//        }
//        log.info(">>> AuthenticationService - start authenticate url={}", requestUri);
//
//        String principal = getLoginName(authentication.getPrincipal());
//
//        List<String> authorizeList = authentication.getAuthorities().stream()
//                .filter(grant -> StrUtil.isNotEmpty(grant.getAuthority()))
//                .map(GrantedAuthority::getAuthority)
//                .collect(Collectors.toList());
//
//        if (authorizeList.size() == 0){
//            log.info(">>> AuthenticationService - 【 {} 】没有任何权限.", principal);
//            return false;
//        }
//
//        log.info(">>> AuthenticationService - principal={}, authorities={}", principal, authorizeList);
//
//
//        RoleListDto params = new RoleListDto();
//        params.setRole_list(authorizeList);
//        IngotResponse<RoleAuthorityDto> response = ucRoleFeignApi.getRoleAuthority(params);
//
//        if (!response.isSuccess()){
//            log.info(">>> AuthenticationService - 获取角色权限失败.");
//            return false;
//        }
//
//        List<String> urlList = response.getData().getUrl_list();
//        log.info(">>> AuthenticationService - 角色权限={}", urlList);
//        // serviceName/path
//        String finalUrl = serviceName.concat(requestUri);
//        log.info(">>> AuthenticationService - final url={}", finalUrl);
//        return urlList.stream().anyMatch(authUrl -> antPathMatcher.match(authUrl, finalUrl));
        return true;
    }

    private String getLoginName(Object principal){
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        if (principal instanceof Principal) {
            return ((Principal) principal).getName();
        }

        return String.valueOf(principal);
    }
}
