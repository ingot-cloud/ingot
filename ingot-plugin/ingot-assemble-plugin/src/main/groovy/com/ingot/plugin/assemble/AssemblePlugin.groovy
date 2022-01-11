package com.ingot.plugin.assemble

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.task.AssembleReleaseTask
import com.ingot.plugin.assemble.task.CleanTask
import com.ingot.plugin.assemble.task.DockerImageTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>Description  : BulidPlugin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/11.</p>
 * <p>Time         : 5:11 PM.</p>
 */
class AssemblePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // create extension
        project.extensions.create("ingotAssemble", AssembleExtension)

        project.afterEvaluate {
            // build task
            AssembleReleaseTask assembleReleaseTask = project.tasks.create("assembleRelease", AssembleReleaseTask)
            // assembleRelease dependsOn assemble
//            assembleReleaseTask.dependsOn project.tasks.getByName("assemble")

            // clean task
            project.tasks.create("cleanAll", CleanTask)

            // 生成 docker image
            project.tasks.create("dockerImage", DockerImageTask)
        }
    }
}
