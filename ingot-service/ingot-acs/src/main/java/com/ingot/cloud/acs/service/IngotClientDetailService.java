package com.ingot.cloud.acs.service;

import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.context.ContextHolder;
import com.ingot.framework.security.constants.OAuthClientDetailSqlConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import static com.ingot.framework.core.constants.BeanIds.CLIENT_DETAIL_SERVICE;

/**
 * <p>Description  : IngotClientDetailService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 3:59 下午.</p>
 */
@Service(CLIENT_DETAIL_SERVICE)
public class IngotClientDetailService extends JdbcClientDetailsService {
    @Resource
    private PasswordEncoder clientDetailPasswordEncoder;

    public IngotClientDetailService(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 增加缓存
     *
     * @param clientId 客户端ID
     * @return ClientDetails
     */
    @Override
    @Cacheable(value = CacheConstants.CLIENT_DETAILS_KEY, key = "#clientId", unless = "#result == null")
    public ClientDetails loadClientByClientId(String clientId) {
        return super.loadClientByClientId(clientId);
    }

    @PostConstruct
    public void init() {
        setSelectClientDetailsSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_SELECT_STATEMENT, ContextHolder.tenantID()));
        setDeleteClientDetailsSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_DELETE_STATEMENT, ContextHolder.tenantID()));
        setUpdateClientDetailsSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_UPDATE_STATEMENT, ContextHolder.tenantID()));
        setUpdateClientSecretSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_UPDATE_SECRET_STATEMENT, ContextHolder.tenantID()));
        setInsertClientDetailsSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_INSERT_STATEMENT, ContextHolder.tenantID()));
        setFindClientDetailsSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_FIND_STATEMENT, ContextHolder.tenantID()));

        setPasswordEncoder(clientDetailPasswordEncoder);
    }
}
