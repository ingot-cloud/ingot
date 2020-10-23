package com.ingot.framework.store.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>Description  : PreconditionsImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-18.</p>
 * <p>Time         : 08:48.</p>
 */
public class PreconditionsImpl<T> implements IService<T> {
    @Override
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(T entity) {
        return false;
    }

    @Override
    public T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Map<String, Object> getMap(Wrapper<T> queryWrapper) {
        return null;
    }

    @Override
    public <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override
    public BaseMapper<T> getBaseMapper() {
        return null;
    }
}
