package com.ingot.framework.core.jackson;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;

import java.math.BigDecimal;

/**
 * <p>Description  : 自定义序列化模块.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/19.</p>
 * <p>Time         : 10:40 下午.</p>
 */
public class InModule extends SimpleModule {
    public InModule() {
        super(InModule.class.getName(), PackageVersion.VERSION);

        this.addSerializer(long.class, new ToStringSerializer());
        this.addSerializer(Long.class, new ToStringSerializer());
        this.addSerializer(BigDecimal.class, new ToStringSerializer());

        this.addSerializer(IPage.class, new IPageSerializer());
    }

}
