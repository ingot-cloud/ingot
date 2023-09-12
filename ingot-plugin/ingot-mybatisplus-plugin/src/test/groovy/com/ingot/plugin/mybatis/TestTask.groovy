package com.ingot.plugin.mybatis

import com.baomidou.mybatisplus.generator.FastAutoGenerator
import com.baomidou.mybatisplus.generator.config.*
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler
import com.ingot.plugin.mybatis.extension.MybatisPlusGeneratorExtension
import org.junit.Test

import java.util.function.Consumer

/**
 * <p>Description  : TestTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/12.</p>
 * <p>Time         : 5:21 PM.</p>
 */
class TestTask {

    @Test
    void testGen() {
        var jdbcUrl = "jdbc:mysql://localhost:3306/ingot_core?useUnicode=true&characterEncoding=UTF-8"
        var jdbcUser = "root"
        var jdbcPassword = "123456"

        var projectPath = "/Users/wangchao/Repositories/github/ingot-cloud/ingot/ingot-plugin/ingot-mybatisplus-plugin"
        var author = "jy"
        var tables = ["sys_user_tenant"]

        var basePackage = "com.ingot.test"

        var generatorExtension = new MybatisPlusGeneratorExtension()

        DataSourceConfig.Builder dataSourceBuilder =
                new DataSourceConfig.Builder(jdbcUrl, jdbcUser, jdbcPassword)
                        .keyWordsHandler(new MySqlKeyWordsHandler())
//                        .dbQuery(new MySqlQuery())
//                        .typeConvert(new MySqlTypeConvert())
//                        .databaseQueryClass(SQLQuery.class)

        FastAutoGenerator.create(dataSourceBuilder)
                .globalConfig(new Consumer<GlobalConfig.Builder>() {
                    @Override
                    void accept(GlobalConfig.Builder builder) {
                        builder.outputDir("${projectPath}/src/main/java")
                        builder.author(author)
                        builder.disableOpenDir()
                    }
                })
                .packageConfig(new Consumer<PackageConfig.Builder>() {
                    @Override
                    void accept(PackageConfig.Builder builder) {
                        builder.parent(basePackage)
                        builder.entity(generatorExtension.entityPackage)
                        builder.controller(generatorExtension.controllerPackage)
                        builder.mapper(generatorExtension.mapperPackage)
                        builder.service(generatorExtension.servicePackage)
                        builder.serviceImpl(generatorExtension.serviceImplPackage)
//                        builder.xml(${tableInfo.getEntityName()}Mapper${StringPool.DOT_XML})
                        builder.pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper/"))
                    }
                })
                .strategyConfig(new Consumer<StrategyConfig.Builder>() {
                    @Override
                    void accept(StrategyConfig.Builder builder) {
                        builder.addInclude(tables)
                        builder.entityBuilder()
                                .superClass(generatorExtension.superEntityClass)
                                .enableLombok()
                                .naming(NamingStrategy.underline_to_camel)
                                .columnNaming(NamingStrategy.underline_to_camel)
                                .formatFileName(generatorExtension.entityName)
                                .enableFileOverride()
                                .build()
                        builder.controllerBuilder()
                                .superClass(generatorExtension.superControllerClass)
                                .enableHyphenStyle()
                                .enableRestStyle()
                                .formatFileName(generatorExtension.controllerName)
                                .enableFileOverride()
                                .build()
                        builder.serviceBuilder()
                                .superServiceClass(generatorExtension.superServiceClass)
                                .superServiceImplClass(generatorExtension.superServiceImplClass)
                                .formatServiceFileName(generatorExtension.serviceName)
                                .formatServiceImplFileName(generatorExtension.serviceImplName)
                                .enableFileOverride()
                                .build()
                        builder.mapperBuilder()
                                .superClass(generatorExtension.superMapperClass)
                                .enableBaseResultMap()
                                .enableBaseColumnList()
                                .formatMapperFileName(generatorExtension.mapperName)
                                .formatXmlFileName(generatorExtension.xmlName)
                                .enableFileOverride()
                                .build()
                    }
                })
                .execute()
    }
}
