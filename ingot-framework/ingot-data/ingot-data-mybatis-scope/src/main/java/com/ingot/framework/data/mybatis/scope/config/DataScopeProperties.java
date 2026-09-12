package com.ingot.framework.data.mybatis.scope.config;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>数据范围与授权快照缓存配置，键归 MyBatis 消费模块。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.mybatis.scope")
public class DataScopeProperties implements Serializable {

    /**
     * 默认部门范围字段名，默认 {@code dept_id}。
     */
    private String scopeFieldName = "dept_id";

    /**
     * 默认归属用户字段名，默认 {@code created_by}；具体表可通过映射覆盖。
     */
    private String userFieldName = "created_by";

    /**
     * 需要进行数据权限过滤的表名；也可由 {@code @DataScopeTable} 自动登记。
     * <p>这些表须具备 {@code (tenant_id, 部门列)} 索引；SELF 热路径另加归属用户列索引。</p>
     */
    private List<String> tables = new ArrayList<>();

    /**
     * 按表覆盖资源编码与列名；禁止把请求参数写入这些标识。
     */
    private List<TableMapping> tableMappings = new ArrayList<>();

    /**
     * 是否启用授权快照 L1，默认开启。
     */
    private boolean cacheEnabled = true;

    /**
     * L1 TTL，默认 30 秒，不得超过授权期限。
     */
    private Duration cacheTtl = Duration.ofSeconds(AuthorizationSnapshotConstants.MAX_TTL_SECONDS);

    /**
     * L1 最大条目数，默认 10000。
     */
    private long cacheMaximumSize = 10_000L;

    /**
     * 是否启用授权快照 L2，默认开启。
     */
    private boolean redisEnabled = true;

    /**
     * L2 TTL，默认 30 秒。
     */
    private Duration redisTtl = Duration.ofSeconds(AuthorizationSnapshotConstants.MAX_TTL_SECONDS);

    /**
     * L2 key 前缀。
     */
    private String redisKeyPrefix = AuthorizationSnapshotConstants.REDIS_KEY_PREFIX;

    /**
     * 按表名查找映射；未配置时返回 {@code null}。
     *
     * @param tableName 表名
     * @return 映射或 {@code null}
     */
    public TableMapping findMapping(String tableName) {
        if (tableName == null || tableMappings == null) {
            return null;
        }
        for (TableMapping mapping : tableMappings) {
            if (tableName.equals(mapping.getTable())) {
                return mapping;
            }
        }
        return null;
    }

    /**
     * 单表的资源与列映射。
     */
    @Getter
    @Setter
    public static class TableMapping implements Serializable {

        /**
         * 表名。
         */
        private String table;

        /**
         * 绑定的资源编码。
         */
        private String resource;

        /**
         * 部门范围列；空则回退默认 {@code scope-field-name}。
         */
        private String scopeColumn;

        /**
         * 归属用户列；空则回退默认 {@code user-field-name}。
         */
        private String userColumn;
    }
}
