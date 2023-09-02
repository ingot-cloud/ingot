package com.ingot.plugin.mybatis.task

import com.baomidou.mybatisplus.generator.FastAutoGenerator
import com.baomidou.mybatisplus.generator.config.DataSourceConfig
import com.baomidou.mybatisplus.generator.config.GlobalConfig
import com.baomidou.mybatisplus.generator.config.PackageConfig
import com.baomidou.mybatisplus.generator.config.StrategyConfig
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler
import com.ingot.plugin.mybatis.extension.IngotMybatisExtension
import com.ingot.plugin.mybatis.extension.MybatisPlusGeneratorExtension
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
//                        builder.pathInfo(Collections.singletonMap(OutputFile.xml, "${projectPath}/src/main/resources/mapper/") as Map<OutputFile, String>)
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
                                .enableBaseResultMap()
                                .enableBaseColumnList()
                                .formatMapperFileName(generatorExtension.mapperName)
                                .formatXmlFileName(generatorExtension.xmlName)
                                .enableFileOverride()
                                .build()
                    }
                })
                .execute()
//
//        // 代码生成器
//        AutoGenerator mpg = new AutoGenerator()
//        // 全局配置
//        GlobalConfig gc = new GlobalConfig()
//        gc.setOutputDir("${projectPath}/src/main/java")
//        gc.setAuthor(generatorExtension.author)
//        gc.setOpen(false)
//        gc.setEntityName(generatorExtension.entityName)
//        gc.setControllerName(generatorExtension.controllerName)
//        gc.setMapperName(generatorExtension.mapperName)
//        gc.setServiceName(generatorExtension.serviceName)
//        gc.setServiceImplName(generatorExtension.serviceImplName)
//        gc.setXmlName(generatorExtension.xmlName)
//        mpg.setGlobalConfig(gc)
//
//        // 数据源配置
//        DataSourceConfig dsc = new DataSourceConfig()
//        dsc.setUrl(ingotMybatisExtension.jdbcUrl)
//        dsc.setDriverName(ingotMybatisExtension.jdbcDriverClass)
//        dsc.setUsername(ingotMybatisExtension.jdbcUser)
//        dsc.setPassword(ingotMybatisExtension.jdbcPassword)
//        mpg.setDataSource(dsc)
//
//        // 包配置
//        PackageConfig pc = new PackageConfig()
//        pc.setParent(generatorExtension.basePackage)
//        pc.setEntity(generatorExtension.entityPackage)
//        pc.setController(generatorExtension.controllerPackage)
//        pc.setMapper(generatorExtension.mapperPackage)
//        pc.setService(generatorExtension.servicePackage)
//        pc.setServiceImpl(generatorExtension.serviceImplPackage)
//        mpg.setPackageInfo(pc)
//
//        // 自定义配置
//        InjectionConfig cfg = new InjectionConfig() {
//            @Override
//            void initMap() {
//                // to do nothing
//            }
//        }
//
//        // 如果模板引擎是 freemarker
//        String templatePath = "/templates/mapper.xml.ftl"
//        // 自定义输出配置
//        List<FileOutConfig> focList = new ArrayList<>()
//        // 自定义配置会被优先输出
//        focList.add(new FileOutConfig(templatePath) {
//            @Override
//            String outputFile(TableInfo tableInfo) {
//                // 自定义输出文件名 ， 如果你 Entity 设置了前后缀、此处注意 xml 的名称会跟着发生变化！！
//                return "${projectPath}/src/main/resources/mapper/${tableInfo.getEntityName()}Mapper${StringPool.DOT_XML}"
//            }
//        })
//        cfg.setFileOutConfigList(focList)
//        mpg.setCfg(cfg)
//
//        // 配置模板
//        TemplateConfig templateConfig = new TemplateConfig()
//
//        // 配置自定义输出模板
//        //指定自定义模板路径，注意不要带上.ftl/.vm, 会根据使用的模板引擎自动识别
//        // templateConfig.setEntity("templates/entity2.java")
//        // templateConfig.setService()
//        // templateConfig.setController()
//
//        templateConfig.setXml(null)
//        mpg.setTemplate(templateConfig)
//
//        // 策略配置
//        StrategyConfig strategy = new StrategyConfig()
//        strategy.setNaming(NamingStrategy.underline_to_camel)
//        strategy.setColumnNaming(NamingStrategy.underline_to_camel)
//        strategy.setSuperEntityClass(generatorExtension.superEntityClass)
//        strategy.setSuperControllerClass(generatorExtension.superControllerClass)
//        strategy.setSuperServiceClass(generatorExtension.superServiceClass)
//        strategy.setSuperServiceImplClass(generatorExtension.superServiceImplClass)
//        if (generatorExtension.superMapperClass != null && !generatorExtension.superMapperClass.isEmpty()) {
//            strategy.setSuperMapperClass(generatorExtension.superMapperClass)
//        }
//        strategy.setEntityLombokModel(true)
//        strategy.setRestControllerStyle(true)
//        strategy.setInclude(generatorExtension.tables)
//        strategy.setControllerMappingHyphenStyle(true)
//        mpg.setStrategy(strategy)
//        mpg.setTemplateEngine(new FreemarkerTemplateEngine())
//        mpg.execute()
    }
}