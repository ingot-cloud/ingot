package com.ingot.framework.security.oauth2.server.authorization.authentication;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * <p>Description  : DelegateUserDetailsTokenProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/31.</p>
 * <p>Time         : 9:31 AM.</p>
 */
@RequiredArgsConstructor
public class DelegateUserDetailsTokenProcessor implements UserDetailsTokenProcessor {
    private final List<UserDetailsTokenProcessor> processors;

    @Override
    public OAuth2UserDetailsAuthenticationToken process(OAuth2UserDetailsAuthenticationToken in) {
        OAuth2UserDetailsAuthenticationToken out;
        for (UserDetailsTokenProcessor processor : processors) {
            if ((out = processor.process(in)) != null) {
                return out;
            }
        }
        return in;
    }
}
