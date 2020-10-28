package com.ingot.framework.security.constants;

/**
 * <p>Description  : OAuthClientDetailSqlConstants.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/2.</p>
 * <p>Time         : 1:54 PM.</p>
 */
public interface OAuthClientDetailSqlConstants {

    /**
     * 数据表名称
     */
    String TABLE_NAME = "sys_oauth_client";

    String CLIENT_FIELDS_FOR_UPDATE = "resource_ids, scope, "
            + "authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, "
            + "refresh_token_validity, additional_information, autoapprove";

    String CLIENT_FIELDS = "client_secret, " + CLIENT_FIELDS_FOR_UPDATE;

    String BASE_FIND_STATEMENT = "select client_id, " + CLIENT_FIELDS
            + " from " + TABLE_NAME;

    String DEFAULT_FIND_STATEMENT = BASE_FIND_STATEMENT + " order by client_id";

    String DEFAULT_SELECT_STATEMENT = BASE_FIND_STATEMENT + " where client_id = ?";

    String DEFAULT_INSERT_STATEMENT = "insert into " + TABLE_NAME + " (" + CLIENT_FIELDS
            + ", client_id) values (?,?,?,?,?,?,?,?,?,?,?)";

    String DEFAULT_UPDATE_STATEMENT = "update " + TABLE_NAME + " set "
            + CLIENT_FIELDS_FOR_UPDATE.replaceAll(", ", "=?, ") + "=? where client_id = ?";

    String DEFAULT_UPDATE_SECRET_STATEMENT = "update " + TABLE_NAME + " set client_secret = ? where client_id = ?";

    String DEFAULT_DELETE_STATEMENT = "delete from " + TABLE_NAME + " where client_id = ?";
}
