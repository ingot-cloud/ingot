package com.ingot.framework.data.mybatis.scope.config;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ingot.framework.data.mybatis.common.annotation.DataScopeTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * <p>Description  : DataScopeTableResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/1.</p>
 * <p>Time         : 16:36.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DataScopeTableResolver implements InitializingBean {
    private final DataScopeProperties properties;

    @Override
    public void afterPropertiesSet() throws Exception {
        List<TableInfo> tableInfoList = TableInfoHelper.getTableInfos();
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getTableName();
            DataScopeTable annotation = AnnotationUtils.findAnnotation(tableInfo.getEntityType(), DataScopeTable.class);
            if (annotation != null && !properties.getTables().contains(tableName)) {
                properties.getTables().add(tableName);
            }
        }

        String tenantTables = CollUtil.isEmpty(properties.getTables()) ? "无" : String.join("\n", properties.getTables());

        log.info("""
                
                
                =============================================
                
                DataScopeTableResolver 数据权限表:
                
                {}
                
                =============================================
                """, tenantTables);
    }
}
