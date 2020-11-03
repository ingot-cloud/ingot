package com.ingot.cloud.acs.service;

import com.ingot.framework.security.constants.OAuthClientDetailSqlConstants;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @PostConstruct
    public void init(){
        setSelectClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_SELECT_STATEMENT);
        setDeleteClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_DELETE_STATEMENT);
        setUpdateClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_UPDATE_STATEMENT);
        setUpdateClientSecretSql(OAuthClientDetailSqlConstants.DEFAULT_UPDATE_SECRET_STATEMENT);
        setInsertClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_INSERT_STATEMENT);
        setFindClientDetailsSql(OAuthClientDetailSqlConstants.DEFAULT_FIND_STATEMENT);

        setPasswordEncoder(clientDetailPasswordEncoder);
    }
}
