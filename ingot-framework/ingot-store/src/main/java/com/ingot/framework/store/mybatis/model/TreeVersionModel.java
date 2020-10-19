package com.ingot.framework.store.mybatis.model;

import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : TreeVersionModel.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 4:59 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TreeVersionModel<T extends Model<?>> extends TreeModel<T> {
    /**
     * 版本号
     */
    @Version
    private Long version;
}
