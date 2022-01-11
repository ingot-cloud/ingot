package com.ingot.plugin.assemble.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
/**
 * <p>Description  : CleanTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-29.</p>
 * <p>Time         : 09:24.</p>
 */
class CleanTask extends DefaultTask {

    CleanTask(){
        setGroup("ingot")
        setDescription("Ingot clean")
    }

    @TaskAction void cleanAll(){
        project.delete(project.projectDir.path + "/out")
        project.delete(project.projectDir.path + "/build")

    }
}
