package com.ingot.framework.data.mybatis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;

/**
 * <p>Description  : BaseServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-18.</p>
 * <p>Time         : 08:43.</p>
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {
}
