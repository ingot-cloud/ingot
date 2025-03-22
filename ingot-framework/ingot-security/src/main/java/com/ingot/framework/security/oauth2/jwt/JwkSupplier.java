package com.ingot.framework.security.oauth2.jwt;

import com.nimbusds.jose.jwk.JWKSet;

import java.util.function.Supplier;

/**
 * <p>Description  : JWK供应商.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/22.</p>
 * <p>Time         : 14:37.</p>
 */
public interface JwkSupplier extends Supplier<JWKSet> {

}
