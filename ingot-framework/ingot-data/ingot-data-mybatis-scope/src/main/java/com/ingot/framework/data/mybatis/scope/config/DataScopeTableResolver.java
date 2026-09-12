package com.ingot.framework.data.mybatis.scope.config;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ingot.framework.data.mybatis.common.annotation.DataScopeTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * <p>启动时把 {@link DataScopeTable} 登记到配置的表名与列映射中。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DataScopeTableResolver implements InitializingBean {
    private final DataScopeProperties properties;

    @Override
    public void afterPropertiesSet() {
        List<TableInfo> tableInfoList = TableInfoHelper.getTableInfos();
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getTableName();
            DataScopeTable annotation = AnnotationUtils.findAnnotation(tableInfo.getEntityType(), DataScopeTable.class);
            if (annotation == null) {
                continue;
            }
            if (!properties.getTables().contains(tableName)) {
                properties.getTables().add(tableName);
            }
            DataScopeProperties.TableMapping existing = properties.findMapping(tableName);
            if (existing == null) {
                DataScopeProperties.TableMapping mapping = new DataScopeProperties.TableMapping();
                mapping.setTable(tableName);
                mapping.setResource(StrUtil.blankToDefault(annotation.resource(), null));
                mapping.setScopeColumn(StrUtil.blankToDefault(annotation.scopeColumn(), null));
                mapping.setUserColumn(StrUtil.blankToDefault(annotation.userColumn(), null));
                properties.getTableMappings().add(mapping);
            } else {
                if (StrUtil.isBlank(existing.getResource()) && StrUtil.isNotBlank(annotation.resource())) {
                    existing.setResource(annotation.resource());
                }
                if (StrUtil.isBlank(existing.getScopeColumn()) && StrUtil.isNotBlank(annotation.scopeColumn())) {
                    existing.setScopeColumn(annotation.scopeColumn());
                }
                if (StrUtil.isBlank(existing.getUserColumn()) && StrUtil.isNotBlank(annotation.userColumn())) {
                    existing.setUserColumn(annotation.userColumn());
                }
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
