package com.ingot.framework.store.mybatis.model;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : BaseModel.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 4:40 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BaseModel<T extends Model<?>> extends Model<T> {

}
