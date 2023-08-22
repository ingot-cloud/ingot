package com.ingot.cloud.auth.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.auth.model.dto.OAuth2AuthorizationDTO;
import com.ingot.cloud.auth.utils.OAuth2AuthorizationUtils;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCache;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import com.ingot.framework.security.oauth2.server.authorization.jackson2.IngotOAuth2AuthorizationServerJackson2Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>Description  : IngotJdbcOAuth2AuthorizationService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 3:07 下午.</p>
 */
@Slf4j
public class IngotJdbcOAuth2AuthorizationService extends JdbcOAuth2AuthorizationService {

    // @formatter:off
    private static final String COLUMN_NAMES = "auth.id, "
            + "auth.registered_client_id, "
            + "auth.principal_name, "
            + "auth.authorization_grant_type, "
            + "auth.authorized_scopes, "
            + "auth.attributes, "
            + "auth.state, "
            + "auth.authorization_code_value, "
            + "auth.authorization_code_issued_at, "
            + "auth.authorization_code_expires_at,"
            + "auth.authorization_code_metadata,"
            + "auth.access_token_value,"
            + "auth.access_token_issued_at,"
            + "auth.access_token_expires_at,"
            + "auth.access_token_metadata,"
            + "auth.access_token_type,"
            + "auth.access_token_scopes,"
            + "auth.oidc_id_token_value,"
            + "auth.oidc_id_token_issued_at,"
            + "auth.oidc_id_token_expires_at,"
            + "auth.oidc_id_token_metadata,"
            + "auth.refresh_token_value,"
            + "auth.refresh_token_issued_at,"
            + "auth.refresh_token_expires_at,"
            + "auth.refresh_token_metadata,"
            + "auth.user_code_value,"
            + "auth.user_code_issued_at,"
            + "auth.user_code_expires_at,"
            + "auth.user_code_metadata,"
            + "auth.device_code_value,"
            + "auth.device_code_issued_at,"
            + "auth.device_code_expires_at,"
            + "auth.device_code_metadata";
    // @formatter:on

    private static final String TABLE_NAME = "oauth2_authorization";

    // @formatter:off
    private static final String LOAD_AUTHORIZATION_SQL = "SELECT " + COLUMN_NAMES
            + " FROM " + TABLE_NAME + " AS auth ";
    // @formatter:on

    private AuthorizationCacheService authorizationCacheService;

    public IngotJdbcOAuth2AuthorizationService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
        super(jdbcOperations, registeredClientRepository);

        OAuth2AuthorizationRowMapper rowMapper = (OAuth2AuthorizationRowMapper) getAuthorizationRowMapper();

        // 自定义 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new IngotOAuth2AuthorizationServerJackson2Module());

        rowMapper.setObjectMapper(objectMapper);
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2AuthorizationUtils.getUser(authorization).ifPresent(user ->
                Optional.ofNullable(authorization.getAccessToken()).ifPresent(accessToken -> {
                    OAuth2AccessToken token = accessToken.getToken();
                    authorizationCacheService.save(
                            user, token.getExpiresAt(),
                            AuthorizationCache.create(
                                    authorization.getId(),
                                    authorization.getRegisteredClientId(),
                                    authorization.getPrincipalName(),
                                    authorization.getAuthorizationGrantType().getValue(),
                                    token.getTokenValue()));
                })
        );
        super.save(authorization);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        OAuth2AuthorizationUtils.getUser(authorization).ifPresent(user -> {
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            authorizationCacheService.remove(user, accessToken.getTokenValue());
        });
        super.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return super.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return super.findByToken(token, tokenType);
    }

    /**
     * 获取授权信息分页信息
     *
     * @param page   分页参数
     * @param params 条件参数
     * @return {@link IPage}
     */
    public IPage<OAuth2AuthorizationDTO> page(IPage<OAuth2AuthorizationDTO> page, OAuth2AuthorizationDTO params) {
        long size = page.getSize();
        long current = page.getCurrent();
        boolean isFirstPage = current == 1;

        String recordSql;
        String countSql = "SELECT count(*) FROM " + TABLE_NAME;
        String where = "";
        PreparedStatementSetter pss = null;
        List<SqlParameterValue> parameters = new ArrayList<>();

        String clientId = params.getRegisteredClientId();
        String name = params.getPrincipalName();
        boolean clientIdIsEmpty = StrUtil.isEmpty(clientId);
        if (!clientIdIsEmpty) {
            where = isFirstPage ? " WHERE registered_client_id = ? "
                    : " WHERE auth_tmp.registered_client_id = ? ";
            parameters.add(new SqlParameterValue(Types.VARCHAR, clientId));
            countSql = countSql + " WHERE registered_client_id = ? ";
        }
        if (StrUtil.isNotEmpty(name)) {
            where += (clientIdIsEmpty ? " WHERE " : " AND ") +
                    (isFirstPage ? " principal_name = ? " : " auth_tmp.principal_name = ? ");
            parameters.add(new SqlParameterValue(Types.VARCHAR, name));
            countSql = countSql + (clientIdIsEmpty ? " WHERE " : " AND ") +
                    " principal_name = ? ";
        }
        if (!parameters.isEmpty()) {
            pss = new ArgumentPreparedStatementSetter(parameters.toArray());
        }

        if (isFirstPage) {
            recordSql = LOAD_AUTHORIZATION_SQL + where + " LIMIT " + size;
        } else {
            recordSql = LOAD_AUTHORIZATION_SQL +
                    "INNER JOIN (" + "SELECT auth_tmp.id FROM " + TABLE_NAME + " AS auth_tmp " + where +
                    " LIMIT " + ((current - 1) * size) + "," + size + ") " +
                    "AS auth_tmp ON auth.id=auth_tmp.id";
        }

        List<OAuth2Authorization> result = this.getJdbcOperations()
                .query(recordSql, pss, this.getAuthorizationRowMapper());
        Long count = this.getJdbcOperations().queryForObject(countSql, Long.class, parameters.toArray());

        List<OAuth2AuthorizationDTO> list = result.stream()
                .map(this::to).collect(Collectors.toList());

        page.setRecords(list);
        page.setTotal(count != null ? count : 0);
        return page;
    }

    @Autowired
    public void setUserDetailsCacheService(AuthorizationCacheService authorizationCacheService) {
        this.authorizationCacheService = authorizationCacheService;
    }

    private OAuth2AuthorizationDTO to(OAuth2Authorization in) {
        OAuth2AuthorizationDTO result = new OAuth2AuthorizationDTO();
        result.setId(in.getId());
        result.setRegisteredClientId(in.getRegisteredClientId());
        result.setPrincipalName(in.getPrincipalName());
        result.setAuthorizationGrantType(in.getAuthorizationGrantType().getValue());
        Instant expiresAt = in.getAccessToken().getToken().getExpiresAt();
        if (expiresAt != null) {
            result.setTokenExpiresAt(
                    LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        }
        Instant issuedAt = in.getAccessToken().getToken().getIssuedAt();
        if (issuedAt != null) {
            result.setTokenIssuedAt(
                    LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()));
        }
        return result;
    }
}
