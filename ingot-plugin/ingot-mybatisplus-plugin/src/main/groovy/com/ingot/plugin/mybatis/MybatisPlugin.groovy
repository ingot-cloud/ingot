package com.ingot.plugin.mybatis

import com.ingot.plugin.mybatis.extension.IngotMybatisExtension
import com.ingot.plugin.mybatis.task.MybatisPlusGeneratorTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>Description  : MybatisPlugin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/10.</p>
 * <p>Time         : 2:46 PM.</p>
 */
class MybatisPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        // create extension
        project.extensions.create('ingotMybatis', IngotMybatisExtension, project.objects)

        project.afterEvaluate {
            project.tasks.register("mybatisPlusGenerator", MybatisPlusGeneratorTask)
        }
    }
}
