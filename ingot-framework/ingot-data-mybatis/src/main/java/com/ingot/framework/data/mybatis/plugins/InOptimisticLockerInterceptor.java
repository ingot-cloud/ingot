package com.ingot.framework.data.mybatis.plugins;

import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.ingot.framework.core.utils.DateUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;

/**
 * <p>Description  : 乐观锁处理.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-17.</p>
 * <p>Time         : 17:18.</p>
 */
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class InOptimisticLockerInterceptor extends OptimisticLockerInnerInterceptor {

    @Override
    protected Object getUpdatedVersionVal(Class<?> clazz, Object originalVersionVal) {
        // 重新处理 Long，使用时间戳
        if (long.class.equals(clazz) || Long.class.equals(clazz)) {
            // UTC time millis
            return DateUtils.utcEpochMilli();
        }
        return super.getUpdatedVersionVal(clazz, originalVersionVal);
    }
}
