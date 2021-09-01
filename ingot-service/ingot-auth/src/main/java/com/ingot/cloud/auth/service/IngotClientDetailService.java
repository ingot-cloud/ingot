package com.ingot.cloud.auth.service;

import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.constants.OAuthClientDetailSqlConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * <p>Description  : IngotClientDetailService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 3:59 下午.</p>
 */
@Slf4j
@Service
public class IngotClientDetailService extends JdbcClientDetailsService {
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
        setSelectClientDetailsSql(String.format(
                OAuthClientDetailSqlConstants.DEFAULT_SELECT_STATEMENT, TenantContextHolder.get()));
        return super.loadClientByClientId(clientId);
    }

    @PostConstruct
    public void init() {
        setSelectClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_SELECT_STATEMENT);
        setDeleteClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_DELETE_STATEMENT);
        setUpdateClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_UPDATE_STATEMENT);
        setUpdateClientSecretSql(OAuthClientDetailSqlConstants.DEFAULT_UPDATE_SECRET_STATEMENT);
        setInsertClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_INSERT_STATEMENT);
        setFindClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_FIND_STATEMENT);
    }
}
