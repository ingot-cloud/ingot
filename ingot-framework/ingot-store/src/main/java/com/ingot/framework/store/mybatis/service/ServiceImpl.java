package com.ingot.framework.store.mybatis.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>Description  : ServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-18.</p>
 * <p>Time         : 08:43.</p>
 */
public class ServiceImpl<M extends BaseMapper<T>, T> extends
        com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<M, T> implements IService<T> {
}
