package com.ingot.framework.core.jackson;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;

/**
 * <p>Description  : IngotModule.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/19.</p>
 * <p>Time         : 10:40 下午.</p>
 */
public class IngotModule extends SimpleModule {
    public IngotModule() {
        super(IngotModule.class.getName(), PackageVersion.VERSION);

        this.addSerializer(long.class, new ToStringSerializer());
        this.addSerializer(Long.class, new ToStringSerializer());
        this.addSerializer(BigDecimal.class, new ToStringSerializer());

        this.addSerializer(IPage.class, new IPageSerializer());
    }

}
