package com.ingot.framework.data.mybatis.config;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ingot.framework.data.mybatis.common.annotation.TenantTable;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.List;

/**
 * <p>Description  : TenantResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/9/4.</p>
 * <p>Time         : 08:16.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TenantResolver implements InitializingBean {
    private final TenantProperties tenantProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        List<TableInfo> tableInfoList = TableInfoHelper.getTableInfos();
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getTableName();
            TenantTable annotation = AnnotationUtils.findAnnotation(tableInfo.getEntityType(), TenantTable.class);
            if (annotation != null && !tenantProperties.getTables().contains(tableName)) {
                tenantProperties.getTables().add(tableName);
            }
        }

        log.info("数据隔离表如下：");
        tenantProperties.getTables().forEach(table -> log.info("表：{}", table));
    }
}
