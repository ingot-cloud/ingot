package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.jackson.InApiTimeModule;
import com.ingot.framework.security.oauth2.jwt.AuthServerJwkSupplier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>使用生产签名装配验证 NumericDate 和 OAuth expires_in 不受 API ISO 编码影响。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class JwtTimeProtocolTest {
    @Test
    void signedJwtAndOauthResponseKeepNumericProtocolAcrossJvmZones() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        var key = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate()).keyID("time-contract").build();
        var supplier = mock(AuthServerJwkSupplier.class);
        when(supplier.getCurrentSigningKey()).thenReturn(key);
        var encoder = new AuthServerJwtEncoderConfiguration().jwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)), supplier);
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
        var mapper = new ObjectMapper().registerModule(new InApiTimeModule());
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(120);
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var claims = JwtClaimsSet.builder().subject("time-contract").issuedAt(issuedAt)
                        .notBefore(issuedAt).expiresAt(expiresAt).build();
                var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build();
                var jwt = encoder.encode(JwtEncoderParameters.from(header, claims));
                var payload = mapper.readTree(SignedJWT.parse(jwt.getTokenValue()).getPayload().toString());
                for (String name : new String[]{"iat", "nbf", "exp"}) {
                    assertTrue(payload.get(name).isIntegralNumber());
                }
                assertEquals(issuedAt.getEpochSecond(), payload.get("iat").longValue());
                assertEquals(expiresAt.getEpochSecond(), payload.get("exp").longValue());
                assertEquals(expiresAt, decoder.decode(jwt.getTokenValue()).getExpiresAt());
                assertEquals('"' + expiresAt.toString() + '"', mapper.writeValueAsString(expiresAt));
                var response = OAuth2AccessTokenResponse.withToken(jwt.getTokenValue())
                        .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(120).build();
                var output = new MockHttpOutputMessage();
                new OAuth2AccessTokenResponseHttpMessageConverter().write(response, MediaType.APPLICATION_JSON, output);
                var json = mapper.readTree(output.getBodyAsBytes());
                assertTrue(json.get("expires_in").isIntegralNumber());
                assertTrue(json.get("expires_in").longValue() > 0 && json.get("expires_in").longValue() <= 120);
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
