package com.ingot.framework.security.provider.token.store;

import java.util.List;

import javax.annotation.PostConstruct;

import com.ingot.framework.security.provider.token.IngotUserAuthenticationConverter;
import com.ingot.framework.security.service.JwtKeyService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * <p>Description  : IngotJwtAccessTokenConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 14:33.</p>
 */
@RequiredArgsConstructor
public class IngotJwtAccessTokenConverter extends JwtAccessTokenConverter {
    private final ResourceServerProperties resource;
    private final List<JwtAccessTokenConverterConfigurer> configurers;
    private final JwtKeyService jwtKeyService;

    @PostConstruct
    public void init(){
        // 自定义 Token Converter
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        UserAuthenticationConverter userTokenConverter = new IngotUserAuthenticationConverter();
        accessTokenConverter.setUserTokenConverter(userTokenConverter);
        setAccessTokenConverter(accessTokenConverter);

        String keyValue = this.resource.getJwt().getKeyValue();
        if (!StringUtils.hasText(keyValue)) {
            keyValue = jwtKeyService.fetchFromCache();
            if (StringUtils.isEmpty(keyValue)){
                throw new BeanCreationException("初始化JwtAccessTokenConverter失败，无法从redis缓存中获取jwt key，请确保鉴权中心是否已启动");
            }
        }
        if (StringUtils.hasText(keyValue) && !keyValue.startsWith("-----BEGIN")) {
            setSigningKey(keyValue);
        }

        // 设置 verifier key
        setVerifierKey(keyValue);

        if (!CollectionUtils.isEmpty(this.configurers)) {
            AnnotationAwareOrderComparator.sort(this.configurers);
            for (JwtAccessTokenConverterConfigurer configurer : this.configurers) {
                configurer.configure(this);
            }
        }
    }
}
