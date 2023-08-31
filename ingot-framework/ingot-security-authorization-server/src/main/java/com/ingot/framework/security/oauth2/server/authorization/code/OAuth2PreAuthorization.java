package com.ingot.framework.security.oauth2.server.authorization.code;

import lombok.Data;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * <p>Description  : PreAuthorization.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/29.</p>
 * <p>Time         : 11:30 AM.</p>
 */
@Data
public class OAuth2PreAuthorization implements Serializable {
    private String id;
    private String registeredClientId;
    private String principalName;
    private AuthorizationGrantType authorizationGrantType;
    private OAuth2PreAuthorizationCode token;
    private Map<String, Object> attributes;

    /**
     * Returns the identifier for the authorization.
     *
     * @return the identifier for the authorization
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the identifier for the {@link RegisteredClient#getId() registered client}.
     *
     * @return the {@link RegisteredClient#getId()}
     */
    public String getRegisteredClientId() {
        return this.registeredClientId;
    }

    /**
     * Returns the {@code Principal} name of the resource owner (or client).
     *
     * @return the {@code Principal} name of the resource owner (or client)
     */
    public String getPrincipalName() {
        return this.principalName;
    }

    /**
     * Returns the {@link AuthorizationGrantType authorization grant type} used for the authorization.
     *
     * @return the {@link AuthorizationGrantType} used for the authorization
     */
    public AuthorizationGrantType getAuthorizationGrantType() {
        return this.authorizationGrantType;
    }

    /**
     * Returns the {@link OAuth2PreAuthorizationCode} used for the authorization.
     *
     * @return the {@link OAuth2PreAuthorizationCode} used for the authorization
     */
    public OAuth2PreAuthorizationCode getToken() {
        return this.token;
    }

    /**
     * Returns the attribute(s) associated to the authorization.
     *
     * @return a {@code Map} of the attribute(s)
     */
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * Returns the value of an attribute associated to the authorization.
     *
     * @param name the name of the attribute
     * @param <T>  the type of the attribute
     * @return the value of an attribute associated to the authorization, or {@code null} if not available
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        Assert.hasText(name, "name cannot be empty");
        return (T) this.attributes.get(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OAuth2PreAuthorization that = (OAuth2PreAuthorization) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.registeredClientId, that.registeredClientId) &&
                Objects.equals(this.principalName, that.principalName) &&
                Objects.equals(this.authorizationGrantType, that.authorizationGrantType) &&
                Objects.equals(this.token, that.token) &&
                Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.registeredClientId, this.principalName,
                this.authorizationGrantType, this.token, this.attributes);
    }

    /**
     * Returns a new {@link OAuth2PreAuthorization.Builder}, initialized with the provided {@link RegisteredClient#getId()}.
     *
     * @param registeredClient the {@link RegisteredClient}
     * @return the {@link OAuth2PreAuthorization.Builder}
     */
    public static OAuth2PreAuthorization.Builder withRegisteredClient(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        return new OAuth2PreAuthorization.Builder(registeredClient.getId());
    }

    /**
     * Returns a new {@link OAuth2PreAuthorization.Builder}, initialized with the values from the provided {@code OAuth2PreAuthorization}.
     *
     * @param authorization the {@code OAuth2PreAuthorization} used for initializing the {@link OAuth2PreAuthorization.Builder}
     * @return the {@link OAuth2PreAuthorization.Builder}
     */
    public static OAuth2PreAuthorization.Builder from(OAuth2PreAuthorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        return new OAuth2PreAuthorization.Builder(authorization.getRegisteredClientId())
                .id(authorization.getId())
                .principalName(authorization.getPrincipalName())
                .authorizationGrantType(authorization.getAuthorizationGrantType())
                .token(authorization.token)
                .attributes(attrs -> attrs.putAll(authorization.getAttributes()));
    }

    public static class Builder implements Serializable {
        private String id;
        private final String registeredClientId;
        private String principalName;
        private AuthorizationGrantType authorizationGrantType;
        private OAuth2PreAuthorizationCode token;
        private final Map<String, Object> attributes = new HashMap<>();

        protected Builder(String registeredClientId) {
            this.registeredClientId = registeredClientId;
        }

        /**
         * Sets the identifier for the authorization.
         *
         * @param id the identifier for the authorization
         * @return the {@link OAuth2PreAuthorization.Builder}
         */
        public OAuth2PreAuthorization.Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the {@code Principal} name of the resource owner (or client).
         *
         * @param principalName the {@code Principal} name of the resource owner (or client)
         * @return the {@link OAuth2PreAuthorization.Builder}
         */
        public OAuth2PreAuthorization.Builder principalName(String principalName) {
            this.principalName = principalName;
            return this;
        }

        /**
         * Sets the {@link AuthorizationGrantType authorization grant type} used for the authorization.
         *
         * @param authorizationGrantType the {@link AuthorizationGrantType}
         * @return the {@link OAuth2PreAuthorization.Builder}
         */
        public OAuth2PreAuthorization.Builder authorizationGrantType(AuthorizationGrantType authorizationGrantType) {
            this.authorizationGrantType = authorizationGrantType;
            return this;
        }

        /**
         * Sets the {@link OAuth2PreAuthorizationCode token}.
         *
         * @param token the {@link OAuth2PreAuthorizationCode}
         * @return the {@link OAuth2PreAuthorization.Builder}
         */
        public OAuth2PreAuthorization.Builder token(OAuth2PreAuthorizationCode token) {
            this.token = token;
            return this;
        }

        /**
         * Adds an attribute associated to the authorization.
         *
         * @param name  the name of the attribute
         * @param value the value of the attribute
         * @return the {@link OAuth2PreAuthorization.Builder}
         */
        public OAuth2PreAuthorization.Builder attribute(String name, Object value) {
            Assert.hasText(name, "name cannot be empty");
            Assert.notNull(value, "value cannot be null");
            this.attributes.put(name, value);
            return this;
        }

        /**
         * A {@code Consumer} of the attributes {@code Map}
         * allowing the ability to add, replace, or remove.
         *
         * @param attributesConsumer a {@link Consumer} of the attributes {@code Map}
         * @return the {@link OAuth2PreAuthorization.Builder}
         */
        public OAuth2PreAuthorization.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
            attributesConsumer.accept(this.attributes);
            return this;
        }

        /**
         * Builds a new {@link OAuth2PreAuthorization}.
         *
         * @return the {@link OAuth2PreAuthorization}
         */
        public OAuth2PreAuthorization build() {
            Assert.hasText(this.principalName, "principalName cannot be empty");
            Assert.notNull(this.authorizationGrantType, "authorizationGrantType cannot be null");

            OAuth2PreAuthorization authorization = new OAuth2PreAuthorization();
            if (!StringUtils.hasText(this.id)) {
                this.id = UUID.randomUUID().toString();
            }
            authorization.id = this.id;
            authorization.registeredClientId = this.registeredClientId;
            authorization.principalName = this.principalName;
            authorization.authorizationGrantType = this.authorizationGrantType;
            authorization.token = this.token;
            authorization.attributes = Collections.unmodifiableMap(this.attributes);
            return authorization;
        }

    }
}
