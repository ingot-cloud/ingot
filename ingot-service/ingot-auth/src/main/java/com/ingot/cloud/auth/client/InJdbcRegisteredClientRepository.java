package com.ingot.cloud.auth.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ingot.framework.core.constants.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * <p>Description  : 自定义{@link RegisteredClientRepository}.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/24.</p>
 * <p>Time         : 4:39 下午.</p>
 */
@Slf4j
@ImportRuntimeHints(InJdbcRegisteredClientRepository.JdbcRegisteredClientRepositoryRuntimeHintsRegistrar.class)
public class InJdbcRegisteredClientRepository implements RegisteredClientRepository {

    // @formatter:off
    private static final String COLUMN_NAMES = "id, "
            + "client_id, "
            + "client_id_issued_at, "
            + "client_secret, "
            + "client_secret_expires_at, "
            + "client_name, "
            + "client_authentication_methods, "
            + "authorization_grant_types, "
            + "redirect_uris, "
            + "post_logout_redirect_uris, "
            + "scopes, "
            + "client_settings,"
            + "token_settings";
    // @formatter:on

    private static final String TABLE_NAME = "oauth2_registered_client";

    private static final String PK_FILTER = "id = ?";

    private static final String LOAD_REGISTERED_CLIENT_SQL = "SELECT " + COLUMN_NAMES + " FROM " + TABLE_NAME + " WHERE ";

    // @formatter:off
    private static final String INSERT_REGISTERED_CLIENT_SQL = "INSERT INTO " + TABLE_NAME
            + "(" + COLUMN_NAMES + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // @formatter:on

    // @formatter:off
    private static final String UPDATE_REGISTERED_CLIENT_SQL = "UPDATE " + TABLE_NAME
            + " SET client_secret = ?, client_secret_expires_at = ?, client_name = ?, client_authentication_methods = ?,"
            + " authorization_grant_types = ?, redirect_uris = ?, post_logout_redirect_uris = ?, scopes = ?,"
            + " client_settings = ?, token_settings = ?"
            + " WHERE " + PK_FILTER;
    // @formatter:on

    private static final String COUNT_REGISTERED_CLIENT_SQL = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ";

    private final JdbcOperations jdbcOperations;
    private RowMapper<RegisteredClient> registeredClientRowMapper;
    private Function<RegisteredClient, List<SqlParameterValue>> registeredClientParametersMapper;

    /**
     * Constructs a {@code JdbcRegisteredClientRepository} using the provided parameters.
     *
     * @param jdbcOperations the JDBC operations
     */
    public InJdbcRegisteredClientRepository(JdbcOperations jdbcOperations) {
        Assert.notNull(jdbcOperations, "jdbcOperations cannot be null");
        this.jdbcOperations = jdbcOperations;
        this.registeredClientRowMapper = new RegisteredClientRowMapper();
        this.registeredClientParametersMapper = new RegisteredClientParametersMapper();
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        RegisteredClient existingRegisteredClient = findBy(PK_FILTER,
                registeredClient.getId());
        if (existingRegisteredClient != null) {
            updateRegisteredClient(registeredClient);
        } else {
            insertRegisteredClient(registeredClient);
        }
    }

    private void updateRegisteredClient(RegisteredClient registeredClient) {
        List<SqlParameterValue> parameters = new ArrayList<>(this.registeredClientParametersMapper.apply(registeredClient));
        SqlParameterValue id = parameters.remove(0);
        parameters.remove(0); // remove client_id
        parameters.remove(0); // remove client_id_issued_at
        parameters.add(id);
        PreparedStatementSetter pss = new ArgumentPreparedStatementSetter(parameters.toArray());
        this.jdbcOperations.update(UPDATE_REGISTERED_CLIENT_SQL, pss);
    }

    private void insertRegisteredClient(RegisteredClient registeredClient) {
        List<SqlParameterValue> parameters = this.registeredClientParametersMapper.apply(registeredClient);
        PreparedStatementSetter pss = new ArgumentPreparedStatementSetter(parameters.toArray());
        this.jdbcOperations.update(INSERT_REGISTERED_CLIENT_SQL, pss);
    }

    private void assertUniqueIdentifiers(RegisteredClient registeredClient) {
        Integer count = this.jdbcOperations.queryForObject(COUNT_REGISTERED_CLIENT_SQL + "client_id = ?", Integer.class,
                registeredClient.getClientId());
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Registered client must be unique. "
                    + "Found duplicate client identifier: " + registeredClient.getClientId());
        }
        if (StringUtils.hasText(registeredClient.getClientSecret())) {
            count = this.jdbcOperations.queryForObject(COUNT_REGISTERED_CLIENT_SQL + "client_secret = ?", Integer.class,
                    registeredClient.getClientSecret());
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Registered client must be unique. "
                        + "Found duplicate client secret for identifier: " + registeredClient.getId());
            }
        }
    }

    @Override
    @Cacheable(value = CacheConstants.CLIENT_DETAILS, key = "#id", unless = "#result == null")
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return findBy("id = ?", id);
    }

    @Override
    @Cacheable(value = CacheConstants.CLIENT_DETAILS, key = "#clientId", unless = "#result == null")
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return findBy("client_id = ?", clientId);
    }

    private RegisteredClient findBy(String filter, Object... args) {
        filter += " and deleted_at IS NULL";
        List<Object> argsList = Lists.newArrayList(args);
        args = argsList.toArray();
        List<RegisteredClient> result = this.jdbcOperations.query(
                LOAD_REGISTERED_CLIENT_SQL + filter, this.registeredClientRowMapper, args);
        return !result.isEmpty() ? result.get(0) : null;
    }

    /**
     * Sets the {@link RowMapper} used for mapping the current row in {@code java.sql.ResultSet} to {@link RegisteredClient}.
     * The default is {@link JdbcRegisteredClientRepository.RegisteredClientRowMapper}.
     *
     * @param registeredClientRowMapper the {@link RowMapper} used for mapping the current row in {@code ResultSet} to {@link RegisteredClient}
     */
    public final void setRegisteredClientRowMapper(RowMapper<RegisteredClient> registeredClientRowMapper) {
        Assert.notNull(registeredClientRowMapper, "registeredClientRowMapper cannot be null");
        this.registeredClientRowMapper = registeredClientRowMapper;
    }

    /**
     * Sets the {@code Function} used for mapping {@link RegisteredClient} to a {@code List} of {@link SqlParameterValue}.
     * The default is {@link JdbcRegisteredClientRepository.RegisteredClientParametersMapper}.
     *
     * @param registeredClientParametersMapper the {@code Function} used for mapping {@link RegisteredClient} to a {@code List} of {@link SqlParameterValue}
     */
    public final void setRegisteredClientParametersMapper(Function<RegisteredClient, List<SqlParameterValue>> registeredClientParametersMapper) {
        Assert.notNull(registeredClientParametersMapper, "registeredClientParametersMapper cannot be null");
        this.registeredClientParametersMapper = registeredClientParametersMapper;
    }

    protected final JdbcOperations getJdbcOperations() {
        return this.jdbcOperations;
    }

    protected final RowMapper<RegisteredClient> getRegisteredClientRowMapper() {
        return this.registeredClientRowMapper;
    }

    protected final Function<RegisteredClient, List<SqlParameterValue>> getRegisteredClientParametersMapper() {
        return this.registeredClientParametersMapper;
    }

    /**
     * The default {@link RowMapper} that maps the current row in
     * {@code java.sql.ResultSet} to {@link RegisteredClient}.
     */
    public static class RegisteredClientRowMapper implements RowMapper<RegisteredClient> {
        private ObjectMapper objectMapper = new ObjectMapper();

        public RegisteredClientRowMapper() {
            ClassLoader classLoader = JdbcRegisteredClientRepository.class.getClassLoader();
            List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
            this.objectMapper.registerModules(securityModules);
            this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        }

        @Override
        public RegisteredClient mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp clientIdIssuedAt = rs.getTimestamp("client_id_issued_at");
            Timestamp clientSecretExpiresAt = rs.getTimestamp("client_secret_expires_at");
            Set<String> clientAuthenticationMethods = StringUtils
                    .commaDelimitedListToSet(rs.getString("client_authentication_methods"));
            Set<String> authorizationGrantTypes = StringUtils
                    .commaDelimitedListToSet(rs.getString("authorization_grant_types"));
            Set<String> redirectUris = StringUtils.commaDelimitedListToSet(rs.getString("redirect_uris"));
            Set<String> postLogoutRedirectUris = StringUtils
                    .commaDelimitedListToSet(rs.getString("post_logout_redirect_uris"));
            Set<String> clientScopes = StringUtils.commaDelimitedListToSet(rs.getString("scopes"));

            // @formatter:off
            RegisteredClient.Builder builder = RegisteredClient.withId(rs.getString("id"))
                    .clientId(rs.getString("client_id"))
                    .clientIdIssuedAt(clientIdIssuedAt != null ? clientIdIssuedAt.toInstant() : null)
                    .clientSecret(rs.getString("client_secret"))
                    .clientSecretExpiresAt(clientSecretExpiresAt != null ? clientSecretExpiresAt.toInstant() : null)
                    .clientName(rs.getString("client_name"))
                    .clientAuthenticationMethods((authenticationMethods) ->
                            clientAuthenticationMethods.forEach(authenticationMethod ->
                                    authenticationMethods.add(resolveClientAuthenticationMethod(authenticationMethod))))
                    .authorizationGrantTypes((grantTypes) ->
                            authorizationGrantTypes.forEach(grantType ->
                                    grantTypes.add(resolveAuthorizationGrantType(grantType))))
                    .redirectUris((uris) -> uris.addAll(redirectUris))
                    .scopes((scopes) -> scopes.addAll(clientScopes));
            // @formatter:on

            Map<String, Object> clientSettingsMap = parseMap(rs.getString("client_settings"));
            builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());

            Map<String, Object> tokenSettingsMap = parseMap(rs.getString("token_settings"));
            TokenSettings.Builder tokenSettingsBuilder = TokenSettings.withSettings(tokenSettingsMap);
            if (!tokenSettingsMap.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT)) {
                tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
            }
            builder.tokenSettings(tokenSettingsBuilder.build());

            return builder.build();
        }

        public final void setObjectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "objectMapper cannot be null");
            this.objectMapper = objectMapper;
        }

        protected final ObjectMapper getObjectMapper() {
            return this.objectMapper;
        }

        private Map<String, Object> parseMap(String data) {
            try {
                return this.objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }

        private static AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
            if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
                return AuthorizationGrantType.AUTHORIZATION_CODE;
            } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
                return AuthorizationGrantType.CLIENT_CREDENTIALS;
            } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
                return AuthorizationGrantType.REFRESH_TOKEN;
            }
            // Custom authorization grant type
            return new AuthorizationGrantType(authorizationGrantType);
        }

        private static ClientAuthenticationMethod resolveClientAuthenticationMethod(String clientAuthenticationMethod) {
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(clientAuthenticationMethod)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            } else if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(clientAuthenticationMethod)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_POST;
            } else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
                return ClientAuthenticationMethod.NONE;
            }
            // Custom client authentication method
            return new ClientAuthenticationMethod(clientAuthenticationMethod);
        }

    }

    /**
     * The default {@code Function} that maps {@link RegisteredClient} to a
     * {@code List} of {@link SqlParameterValue}.
     */
    public static class RegisteredClientParametersMapper
            implements Function<RegisteredClient, List<SqlParameterValue>> {
        private ObjectMapper objectMapper = new ObjectMapper();
        private PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        public RegisteredClientParametersMapper() {
            ClassLoader classLoader = JdbcRegisteredClientRepository.class.getClassLoader();
            List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
            this.objectMapper.registerModules(securityModules);
            this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        }

        @Override
        public List<SqlParameterValue> apply(RegisteredClient registeredClient) {
            Timestamp clientIdIssuedAt = (registeredClient.getClientIdIssuedAt() != null)
                    ? Timestamp.from(registeredClient.getClientIdIssuedAt()) : Timestamp.from(Instant.now());

            Timestamp clientSecretExpiresAt = (registeredClient.getClientSecretExpiresAt() != null)
                    ? Timestamp.from(registeredClient.getClientSecretExpiresAt()) : null;

            List<String> clientAuthenticationMethods = new ArrayList<>(
                    registeredClient.getClientAuthenticationMethods().size());
            registeredClient.getClientAuthenticationMethods()
                    .forEach((clientAuthenticationMethod) -> clientAuthenticationMethods
                            .add(clientAuthenticationMethod.getValue()));

            List<String> authorizationGrantTypes = new ArrayList<>(
                    registeredClient.getAuthorizationGrantTypes().size());
            registeredClient.getAuthorizationGrantTypes()
                    .forEach((authorizationGrantType) -> authorizationGrantTypes.add(authorizationGrantType.getValue()));

            return Arrays.asList(
                    new SqlParameterValue(Types.VARCHAR, registeredClient.getId()),
                    new SqlParameterValue(Types.VARCHAR, registeredClient.getClientId()),
                    new SqlParameterValue(Types.TIMESTAMP, clientIdIssuedAt),
                    new SqlParameterValue(Types.VARCHAR, encode(registeredClient.getClientSecret())),
                    new SqlParameterValue(Types.TIMESTAMP, clientSecretExpiresAt),
                    new SqlParameterValue(Types.VARCHAR, registeredClient.getClientName()),
                    new SqlParameterValue(Types.VARCHAR,
                            StringUtils.collectionToCommaDelimitedString(clientAuthenticationMethods)),
                    new SqlParameterValue(Types.VARCHAR,
                            StringUtils.collectionToCommaDelimitedString(authorizationGrantTypes)),
                    new SqlParameterValue(Types.VARCHAR,
                            StringUtils.collectionToCommaDelimitedString(registeredClient.getRedirectUris())),
                    new SqlParameterValue(Types.VARCHAR,
                            StringUtils.collectionToCommaDelimitedString(registeredClient.getPostLogoutRedirectUris())),
                    new SqlParameterValue(Types.VARCHAR,
                            StringUtils.collectionToCommaDelimitedString(registeredClient.getScopes())),
                    new SqlParameterValue(Types.VARCHAR, writeMap(registeredClient.getClientSettings().getSettings())),
                    new SqlParameterValue(Types.VARCHAR, writeMap(registeredClient.getTokenSettings().getSettings())));
        }

        public final void setObjectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "objectMapper cannot be null");
            this.objectMapper = objectMapper;
        }


        public final void setPasswordEncoder(PasswordEncoder passwordEncoder) {
            Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
            this.passwordEncoder = passwordEncoder;
        }

        protected final ObjectMapper getObjectMapper() {
            return this.objectMapper;
        }

        private String writeMap(Map<String, Object> data) {
            try {
                return this.objectMapper.writeValueAsString(data);
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }

        private String encode(String value) {
            if (value != null) {
                return this.passwordEncoder.encode(value);
            }
            return null;
        }

    }

    static class JdbcRegisteredClientRepositoryRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources()
                    .registerResource(new ClassPathResource(
                            "org/springframework/security/oauth2/server/authorization/client/oauth2-registered-client-schema.sql"));
        }

    }
}
