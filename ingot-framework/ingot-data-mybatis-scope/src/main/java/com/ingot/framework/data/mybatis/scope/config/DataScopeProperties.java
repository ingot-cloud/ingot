package com.ingot.framework.data.mybatis.scope.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : DataScope.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 15:42.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.mybatis.scope")
public class DataScopeProperties implements Serializable {

    /**
     * 数据范围字段名
     */
    private String scopeFieldName = "dept_id";

    /**
     * 用户字段名
     */
    private String userFieldName = "created_by";

    /**
     * 需要进行数据权限过滤的表
     */
    private List<String> tables = new ArrayList<>();
}
