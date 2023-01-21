package com.ingot.framework.store.mybatis.common;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * <p>Description  : PageUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/1/1.</p>
 * <p>Time         : 12:18 PM.</p>
 */
public class PageUtils {

    /**
     * Page map
     *
     * @param inPage {@link IPage}
     * @param map    {@link Function}
     * @param <In>
     * @param <Out>
     * @return {@link IPage}
     */
    public static <In, Out> IPage<Out> map(IPage<In> inPage, Function<In, Out> map) {
        IPage<Out> result = new Page<>();
        result.setCurrent(inPage.getCurrent());
        result.setTotal(inPage.getTotal());
        result.setSize(inPage.getSize());
        result.setRecords(inPage.getRecords()
                .stream().map(map).collect(Collectors.toList()));
        return result;
    }

    public static <In, Out> IPage<Out> mapAndSorted(IPage<In> inPage, Function<In, Out> map, Comparator<? super Out> comparator) {
        IPage<Out> result = new Page<>();
        result.setCurrent(inPage.getCurrent());
        result.setTotal(inPage.getTotal());
        result.setSize(inPage.getSize());
        result.setRecords(inPage.getRecords()
                .stream()
                .map(map)
                .sorted(comparator).collect(Collectors.toList()));
        return result;
    }
}
