package com.ingot.framework.data.scope;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Map;

/**
 * <p>Description  : DefaultDataScopeInterceptor.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 16:37.</p>
 */
@RequiredArgsConstructor
public class DefaultDataScopeInterceptor implements InDataScopeInterceptor {
    private final DataScopeCalcHandler dataScopeCalcHandler;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);

        String originalSql = boundSql.getSql();
        Object parameterObject = boundSql.getParameterObject();

        // 查找参数中包含DataScope类型的参数
        DataScope dataScope = findDataScopeObject(parameterObject);
        if (dataScope == null) {
            return;
        }

        // 是否强制跳过数据权限
        if (dataScope.isSkip()) {
            return;
        }

//        // 返回true 不拦截直接返回原始 SQL （只针对 * 查询）
//        if (DataScopeFuncEnum.ALL.equals(dataScope.getFunc()) && dataScopeHandle.calcScope(dataScope)) {
//            return;
//        }
//
//        // 返回true 不拦截直接返回原始 SQL （只针对 COUNT 查询）
//        if (DataScopeFuncEnum.COUNT.equals(dataScope.getFunc()) && dataScopeHandle.calcScope(dataScope)) {
//            mpBs.sql(String.format("SELECT %s FROM (%s) temp_data_scope", dataScope.getFunc().getType(), originalSql));
//            return;
//        }
//
//        List<Long> deptIds = dataScope.getDeptList();
//
//        // 1.无数据权限限制，则直接返回 0 条数据
//        if (CollUtil.isEmpty(deptIds) && StrUtil.isBlank(dataScope.getUsername())) {
//            originalSql = String.format("SELECT %s FROM (%s) temp_data_scope WHERE 1 = 2",
//                    dataScope.getFunc().getType(), originalSql);
//        }
//        // 2.如果为本人权限 + 部门权限控制
//        else if (StrUtil.isNotBlank(dataScope.getUsername()) && CollUtil.isNotEmpty(deptIds)) {
//            String join = CollectionUtil.join(deptIds, ",");
//            originalSql = String.format("SELECT %s FROM (%s) temp_data_scope WHERE temp_data_scope.%s = '%s' OR temp_data_scope.%s IN (%s)",
//                    dataScope.getFunc().getType(), originalSql, dataScope.getScopeUserName(), dataScope.getUsername(), dataScope.getScopeDeptName(), join);
//        }
//        // 3. 如果为本人
//        else if (StrUtil.isNotBlank(dataScope.getUsername())) {
//            originalSql = String.format("SELECT %s FROM (%s) temp_data_scope WHERE temp_data_scope.%s = '%s'",
//                    dataScope.getFunc().getType(), originalSql, dataScope.getScopeUserName(), dataScope.getUsername());
//        }
//        // 4.部门权限控制
//        else {
//            String join = CollectionUtil.join(deptIds, ",");
//            originalSql = String.format("SELECT %s FROM (%s) temp_data_scope WHERE temp_data_scope.%s IN (%s)",
//                    dataScope.getFunc().getType(), originalSql, dataScope.getScopeDeptName(), join);
//        }

        mpBs.sql(originalSql);
    }

    /**
     * 查找参数是否包括DataScope对象
     *
     * @param parameterObj 参数列表
     * @return DataScope
     */
    private DataScope findDataScopeObject(Object parameterObj) {
        if (parameterObj instanceof DataScope) {
            return (DataScope) parameterObj;
        } else if (parameterObj instanceof Map) {
            for (Object val : ((Map<?, ?>) parameterObj).values()) {
                if (val instanceof DataScope) {
                    return (DataScope) val;
                }
            }
        }
        return null;
    }
}
