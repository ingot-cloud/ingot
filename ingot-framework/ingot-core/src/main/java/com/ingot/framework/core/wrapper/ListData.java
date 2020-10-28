package com.ingot.framework.core.wrapper;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : ListData.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/12.</p>
 * <p>Time         : 下午3:18.</p>
 */
@Deprecated
@Builder
public class ListData<T> implements Serializable {
    @Getter
    private long total;
    @Getter
    private List<T> list;
    @Getter
    private long page_num;

    public static <T> ListData<T> of(List<T> list) {
        return ListData.<T>builder()
                .list(list)
                .total(list.size())
                .build();
    }

    public static <T> ListData<T> of(long total, List<T> list) {
        return ListData.<T>builder()
                .list(list)
                .total(total)
                .build();
    }

    public static <T> ListData<T> of(long total, long pageNum, List<T> list) {
        return ListData.<T>builder()
                .list(list)
                .total(total)
                .page_num(pageNum)
                .build();
    }

//    public static <T> ListData<T> of(IPage<T> page) {
//        return ListData.<T>builder()
//                .list(page.getRecords())
//                .total(page.getTotal())
//                .page_num(page.getCurrent())
//                .build();
//    }
}
