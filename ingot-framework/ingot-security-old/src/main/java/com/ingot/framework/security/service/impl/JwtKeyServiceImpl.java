package com.ingot.framework.security.service.impl;

import java.util.List;
import java.util.Map;

import com.ingot.framework.security.common.utils.SecurityUtils;
import com.ingot.framework.security.service.JwtKeyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * <p>Description  : JwtKeyServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 11:41.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtKeyServiceImpl implements JwtKeyService {
    private final ResourceServerProperties resource;
    private final List<JwtAccessTokenConverterRestTemplateCustomizer> customizers;
    private final RestTemplate lbRestTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String fetch() {
        return getKeyFromServer();
    }

    @Override
    public String fetchFromCache() {
        return "";
//        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----", redisTemplate.opsForValue().get(RedisConstants.REDIS_JWT_PUB_KEY));
    }

    /**
     * Fix getKeyFromServer, RestTemplate LoadBalanced
     */
    private String getKeyFromServer() {
        RestTemplate keyUriRestTemplate = lbRestTemplate;
        if (!CollectionUtils.isEmpty(this.customizers)) {
            for (JwtAccessTokenConverterRestTemplateCustomizer customizer : this.customizers) {
                customizer.customize(keyUriRestTemplate);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        String username = this.resource.getClientId();
        String password = this.resource.getClientSecret();
        headers.add("Authorization", "Basic " + SecurityUtils.makeBasicToken(username, password));

        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = this.resource.getJwt().getKeyUri();
        log.info(">>> lbRestTemplate={}, url={}", lbRestTemplate, url);
        return (String) keyUriRestTemplate
                .exchange(url, HttpMethod.GET, request, Map.class).getBody()
                .get("value");
    }
}
