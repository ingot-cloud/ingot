package com.ingot.framework.data.mybatis.common.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.data.mybatis.common.ser.PaginationJackson2Serializer;

/**
 * <p>Description  : Pagination.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/29.</p>
 * <p>Time         : 9:53 下午.</p>
 */
@JsonSerialize(using = PaginationJackson2Serializer.class)
public class Pagination<T> extends Page<T> {
}
