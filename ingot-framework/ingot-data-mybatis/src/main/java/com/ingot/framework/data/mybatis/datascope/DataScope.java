package com.ingot.framework.data.mybatis.datascope;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : DataScope.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 15:42.</p>
 */
@Data
public class DataScope implements Serializable {
    /**
     * 跳过数据权限过滤
     */
    private boolean skip = false;

    /**
     * 数据范围字段名
     */
    private String scopeFieldName = "dept_id";

    /**
     * 用户字段名
     */
    private String userFieldName = "created_by";

    /**
     * 数据范围
     */
    private List<Long> scopes = new ArrayList<>();
    /**
     * 用户权限范围
     */
    private Long userScope;

}
