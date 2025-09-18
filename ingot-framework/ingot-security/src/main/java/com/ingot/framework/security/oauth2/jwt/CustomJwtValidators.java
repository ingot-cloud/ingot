package com.ingot.framework.security.oauth2.jwt;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.security.core.InSecurityProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/**
 * <p>Description  : 自定义JwtValidators.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 1:51 下午.</p>
 */
public final class CustomJwtValidators {
    private CustomJwtValidators() {
    }

    /**
     * <p>
     * Create a {@link Jwt} Validator that contains all standard validators when an issuer
     * is known.
     * </p>
     * <p>
     * User's wanting to leverage the defaults plus additional validation can add the
     * result of this method to {@code DelegatingOAuth2TokenValidator} along with the
     * additional validators.
     * </p>
     *
     * @param issuer the issuer
     * @return - a delegating validator containing all standard validators as well as any
     * supplied
     */
    public static OAuth2TokenValidator<Jwt> createDefaultWithIssuer(String issuer,
                                                                    InSecurityProperties properties) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtIssuerValidator(issuer));
        validators.add(new JwtTenantValidator(properties));
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    /**
     * 创建一个{@link Jwt}验证器，包含所有标准验证器。
     *
     * @param properties {@link InSecurityProperties}
     * @return {@link OAuth2TokenValidator}
     */
    public static OAuth2TokenValidator<Jwt> createDefault(InSecurityProperties properties) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtTenantValidator(properties));
        return new DelegatingOAuth2TokenValidator<>(validators);
    }
}
