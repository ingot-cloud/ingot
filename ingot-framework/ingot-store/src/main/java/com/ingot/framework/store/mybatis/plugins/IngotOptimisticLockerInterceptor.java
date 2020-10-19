package com.ingot.framework.store.mybatis.plugins;

import com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor;
import com.ingot.framework.base.utils.DateUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;

/**
 * <p>Description  : IngotOptimisticLockerInterceptor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-17.</p>
 * <p>Time         : 17:18.</p>
 */
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class IngotOptimisticLockerInterceptor extends OptimisticLockerInterceptor {

    @Override protected Object getUpdatedVersionVal(Object originalVersionVal) {
        Class<?> versionValClass = originalVersionVal.getClass();
        // 重新处理 Long，使用时间戳
        if (Long.class.equals(versionValClass)) {
            // UTC time millis
            return DateUtils.utc().getTime();
        }
        return super.getUpdatedVersionVal(originalVersionVal);
    }
}
