package com.ingot.plugin.mybatis.task

import com.baomidou.mybatisplus.generator.FastAutoGenerator
import com.baomidou.mybatisplus.generator.config.*
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler
import com.ingot.plugin.mybatis.extension.IngotMybatisExtension
import com.ingot.plugin.mybatis.extension.MybatisPlusGeneratorExtension
import org.apache.ibatis.annotations.Mapper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.util.function.Consumer

/**
 * <p>Description  : MybatisPlusGeneratorTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-23.</p>
 * <p>Time         : 14:49.</p>
 */
class MybatisPlusGeneratorTask extends DefaultTask {

    MybatisPlusGeneratorTask() {
        setGroup("ingot")
        setDescription("Mybatis Plus Generator")
    }

    @TaskAction
    void generator() {
        IngotMybatisExtension ingotMybatisExtension = IngotMybatisExtension.getBuildExtension(project)
        MybatisPlusGeneratorExtension generatorExtension = ingotMybatisExtension.mybatisPlus
        String projectPath = generatorExtension.projectPath

        if (projectPath == null || projectPath.isEmpty()) {
            throw new GradleException("请配置projectPath")
        }

        if (generatorExtension.basePackage == null || generatorExtension.basePackage.isEmpty()) {
            throw new GradleException("请配置basePackage")
        }

        if (generatorExtension.tables == null || generatorExtension.tables.length == 0) {
            throw new GradleException("请配置tables")
        }

        project.logger.lifecycle("projectPath:" + projectPath)
        project.logger.lifecycle("basePackage:" + generatorExtension.basePackage)
        project.logger.lifecycle("tables:" + generatorExtension.tables)

        DataSourceConfig.Builder dataSourceBuilder =
                new DataSourceConfig.Builder(ingotMybatisExtension.jdbcUrl, ingotMybatisExtension.jdbcUser, ingotMybatisExtension.jdbcPassword)
                        .keyWordsHandler(new MySqlKeyWordsHandler())
//                        .dbQuery(new MySqlQuery())
//                        .typeConvert(new MySqlTypeConvert())
//                        .databaseQueryClass(SQLQuery.class)

        FastAutoGenerator.create(dataSourceBuilder)
                .globalConfig(new Consumer<GlobalConfig.Builder>() {
                    @Override
                    void accept(GlobalConfig.Builder builder) {
                        builder.outputDir("${projectPath}/src/main/java")
                        builder.author(generatorExtension.author)
                        builder.disableOpenDir()
                    }
                })
                .packageConfig(new Consumer<PackageConfig.Builder>() {
                    @Override
                    void accept(PackageConfig.Builder builder) {
                        builder.parent(generatorExtension.basePackage)
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
                        builder.addInclude(generatorExtension.tables)
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
                                .mapperAnnotation(Mapper.class)
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