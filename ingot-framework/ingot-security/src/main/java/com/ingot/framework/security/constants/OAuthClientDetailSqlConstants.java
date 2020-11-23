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
    String TABLE_NAME = "sys_oauth_client_details";

    /**
     * 可更新的字段，不包括 client_id、client_secret
     */
    String CLIENT_FIELDS_FOR_UPDATE = "resource_ids, scope, "
            + "authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, "
            + "refresh_token_validity, additional_information, autoapprove";

    /**
     * 查询字段，包含 client_id 和 client_secret
     */
    String BASE_FIND_STATEMENT = "select client_id, CONCAT('{noop}',client_secret) as client_secret, "
            + CLIENT_FIELDS_FOR_UPDATE + " from " + TABLE_NAME;

    /**
     * 查找语句
     */
    String DEFAULT_FIND_STATEMENT = BASE_FIND_STATEMENT + " order by client_id " +
            "where deleted_at is not null and tenant_id = %s";

    /**
     * Select
     */
    String DEFAULT_SELECT_STATEMENT = BASE_FIND_STATEMENT + " where client_id = ? " +
            "and deleted_at is not null and tenant_id = %s";

    /**
     * Insert
     */
    String DEFAULT_INSERT_STATEMENT = "insert into " + TABLE_NAME + " (" + CLIENT_FIELDS_FOR_UPDATE
            + ", client_id, client_secret, tenant_id) values (?,?,?,?,?,?,?,?,?,?,?,%s)";

    /**
     * Update
     */
    String DEFAULT_UPDATE_STATEMENT = "update " + TABLE_NAME + " set "
            + CLIENT_FIELDS_FOR_UPDATE.replaceAll(", ", "=?, ") + "=? " +
            "where client_id = ? and tenant_id = %s";

    /**
     * Update client_secret
     */
    String DEFAULT_UPDATE_SECRET_STATEMENT = "update " + TABLE_NAME + " set client_secret = ? " +
            "where client_id = ? and tenant_id = %s";

    /**
     * Delete
     */
    String DEFAULT_DELETE_STATEMENT = "delete from " + TABLE_NAME +
            " where client_id = ? and tenant_id = %s";
}
