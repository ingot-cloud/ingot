package com.ingot.framework.security.oauth2.jwt;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

/**
 * <p>Description  : JwtTenantValidator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 1:59 下午.</p>
 */
public class JwtTenantValidator implements OAuth2TokenValidator<Jwt> {

//    private final JwtClaimValidator<Object> validator;

    public JwtTenantValidator() {
//        Predicate<Object> testClaimValue = (claimValue) -> (claimValue != null) && issuer.equals(claimValue.toString());
//        this.validator = new JwtClaimValidator<>(ExtensionClaimNames.TENANT, testClaimValue);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Assert.notNull(token, "token cannot be null");
//        return this.validator.validate(token);
        return OAuth2TokenValidatorResult.success();
    }
}
