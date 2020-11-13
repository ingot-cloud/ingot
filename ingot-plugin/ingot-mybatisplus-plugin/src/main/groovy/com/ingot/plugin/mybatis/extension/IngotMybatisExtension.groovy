package com.ingot.plugin.mybatis.extension

import org.gradle.api.Action
import org.gradle.api.Project
/**
 * <p>Description  : IngotMybatisExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-23.</p>
 * <p>Time         : 14:42.</p>
 */
class IngotMybatisExtension {

    /**
     * 指定连接数据库的驱动
     */
    String jdbcDriverClass

    /**
     * 连接池连接地址
     */
    String jdbcUrl

    /**
     * 连接池连接账号
     */
    String jdbcUser

    /**
     * 连接池连接密码
     */
    String jdbcPassword

    /**
     * Mybatis Plus 配置
     */
    MybatisPlusGeneratorExtension mybatisPlus = new MybatisPlusGeneratorExtension()

    IngotMybatisExtension(){

    }

    void mybatisPlus(Action<MybatisPlusGeneratorExtension> action){
        action.execute(this.mybatisPlus)
    }

//    //创建内部Extension，名称为方法名 mybatisPlus
//    void mybatisPlus(Closure c) {
//        ConfigureUtil.configure(c, mybatisPlus)
//    }

    static IngotMybatisExtension getBuildExtension(Project project){
        IngotMybatisExtension ingotMybatisExtension = project.getExtensions().findByType(IngotMybatisExtension)
        if (ingotMybatisExtension == null){
            ingotMybatisExtension = new IngotMybatisExtension()
        }

        return ingotMybatisExtension
    }
}
