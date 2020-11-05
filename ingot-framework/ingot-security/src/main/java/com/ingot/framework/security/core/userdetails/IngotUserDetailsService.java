package com.ingot.framework.security.core.userdetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * <p>Description  : IngotUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/11.</p>
 * <p>Time         : 4:33 PM.</p>
 */
public interface IngotUserDetailsService extends UserDetailsService {

    /**
     * 根据社交登录 openId 获取 UserDetails
     *
     * @param socialType 社交类型
     * @param openId     社交登录唯一Id
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    UserDetails loadUserBySocial(String socialType, String openId) throws UsernameNotFoundException;
}
