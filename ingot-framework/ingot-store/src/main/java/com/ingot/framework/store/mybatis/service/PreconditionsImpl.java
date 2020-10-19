package com.ingot.framework.store.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>Description  : PreconditionsImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-18.</p>
 * <p>Time         : 08:48.</p>
 */
public class PreconditionsImpl<T> implements IService<T> {
    @Override public boolean save(T entity) {
        return false;
    }

    @Override public boolean saveBatch(Collection<T> entityList, int batchSize) {
        return false;
    }

    @Override public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        return false;
    }

    @Override public boolean removeById(Serializable id) {
        return false;
    }

    @Override public boolean removeByMap(Map<String, Object> columnMap) {
        return false;
    }

    @Override public boolean remove(Wrapper<T> queryWrapper) {
        return false;
    }

    @Override public boolean removeByIds(Collection<? extends Serializable> idList) {
        return false;
    }

    @Override public boolean updateById(T entity) {
        return false;
    }

    @Override public boolean update(T entity, Wrapper<T> updateWrapper) {
        return false;
    }

    @Override public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        return false;
    }

    @Override public boolean saveOrUpdate(T entity) {
        return false;
    }

    @Override public T getById(Serializable id) {
        return null;
    }

    @Override public Collection<T> listByIds(Collection<? extends Serializable> idList) {
        return null;
    }

    @Override public Collection<T> listByMap(Map<String, Object> columnMap) {
        return null;
    }

    @Override public T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override public Map<String, Object> getMap(Wrapper<T> queryWrapper) {
        return null;
    }

    @Override public <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override public int count(Wrapper<T> queryWrapper) {
        return 0;
    }

    @Override public List<T> list(Wrapper<T> queryWrapper) {
        return null;
    }

    @Override public IPage<T> page(IPage<T> page, Wrapper<T> queryWrapper) {
        return null;
    }

    @Override public List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper) {
        return null;
    }

    @Override public <V> List<V> listObjs(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override public IPage<Map<String, Object>> pageMaps(IPage<T> page, Wrapper<T> queryWrapper) {
        return null;
    }

    @Override public BaseMapper<T> getBaseMapper() {
        return null;
    }
}
