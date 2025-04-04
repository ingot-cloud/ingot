package com.ingot.plugin.mybatis.extension
/**
 * <p>Description  : MybatisPlusGeneratorExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-23.</p>
 * <p>Time         : 14:47.</p>
 */
class MybatisPlusGeneratorExtension {

    /**
     * 项目路径
     */
    String projectPath

    /**
     * 注释作者
     */
    String author = "jymot"

    /**
     * 需要生成的表
     */
    String[] tables

    /**
     * 包名
     */
    String basePackage

    /**
     * 保存 entity 的相对包名
     */
    String entityPackage = "model.domain"

    /**
     * 保存 controller 的相对包名
     */
    String controllerPackage = "web"

    /**
     * 保存 mapper 的相对包名
     */
    String mapperPackage = "mapper"

    /**
     * 保存 service 的相对包名
     */
    String servicePackage = "service.domain"

    /**
     * 保存 serviceImpl 的相对包名
     */
    String serviceImplPackage = "service.domain.impl"

    /**
     * entity 文件名
     */
    String entityName = "%s"

    /**
     * controller 文件名
     */
    String controllerName = "%sController"

    /**
     * mapper 文件名
     */
    String mapperName = "%sMapper"

    /**
     * service 文件名
     */
    String serviceName = "%sService"

    /**
     * serviceImpl 文件名
     */
    String serviceImplName = "%sServiceImpl"

    /**
     * xml mapper 文件名
     */
    String xmlName = "%sMapper"

    /**
     * entity 父类
     */
    String superEntityClass = "com.ingot.framework.data.mybatis.common.model.BaseModel"

    /**
     * controller 父类
     */
    String superControllerClass = "com.ingot.framework.core.model.support.RShortcuts"

    /**
     * service 父类
     */
    String superServiceClass = "com.ingot.framework.data.mybatis.common.service.BaseService"

    /**
     * serviceImpl 父类
     */
    String superServiceImplClass = "com.ingot.framework.data.mybatis.common.service.BaseServiceImpl"

    /**
     * mapper 父类
     */
    String superMapperClass = "com.ingot.framework.data.mybatis.common.mapper.BaseMapper"

    MybatisPlusGeneratorExtension() {

    }
}
