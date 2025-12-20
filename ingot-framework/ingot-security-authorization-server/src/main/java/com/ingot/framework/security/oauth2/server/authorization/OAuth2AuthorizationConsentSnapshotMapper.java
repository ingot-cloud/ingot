package com.ingot.framework.security.oauth2.server.authorization;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2AuthorizationConsent 和 OAuth2AuthorizationConsentSnapshot 之间的转换工具
 *
 * <p>Author: jy</p>
 * <p>Date: 2024/12/17</p>
 */
public class OAuth2AuthorizationConsentSnapshotMapper {

    /**
     * 将 OAuth2AuthorizationConsent 转换为 OAuth2AuthorizationConsentSnapshot
     */
    public static OAuth2AuthorizationConsentSnapshot toSnapshot(OAuth2AuthorizationConsent consent) {
        if (consent == null) {
            return null;
        }

        OAuth2AuthorizationConsentSnapshot snapshot = new OAuth2AuthorizationConsentSnapshot();

        // 基本信息
        snapshot.setRegisteredClientId(consent.getRegisteredClientId());
        snapshot.setPrincipalName(consent.getPrincipalName());

        // 权限列表：GrantedAuthority → String
        Set<String> authorities = consent.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        snapshot.setAuthorities(authorities);

        return snapshot;
    }

    /**
     * 将 OAuth2AuthorizationConsentSnapshot 转换为 OAuth2AuthorizationConsent
     *
     * @param snapshot Snapshot 对象
     * @param registeredClientRepository RegisteredClient仓库，用于获取客户端信息
     * @return OAuth2AuthorizationConsent 对象
     */
    public static OAuth2AuthorizationConsent fromSnapshot(
            OAuth2AuthorizationConsentSnapshot snapshot,
            RegisteredClientRepository registeredClientRepository) {
        if (snapshot == null) {
            return null;
        }

        // 获取 RegisteredClient
        RegisteredClient registeredClient = registeredClientRepository.findById(snapshot.getRegisteredClientId());
        if (registeredClient == null) {
            throw new IllegalStateException("RegisteredClient not found: " + snapshot.getRegisteredClientId());
        }

        // 使用 Builder API 构建
        OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent
                .withId(snapshot.getRegisteredClientId(), snapshot.getPrincipalName());

        // 权限列表：String → GrantedAuthority
        if (snapshot.getAuthorities() != null && !snapshot.getAuthorities().isEmpty()) {
            Set<GrantedAuthority> authorities = snapshot.getAuthorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            authorities.forEach(builder::authority);
        }

        return builder.build();
    }
}
