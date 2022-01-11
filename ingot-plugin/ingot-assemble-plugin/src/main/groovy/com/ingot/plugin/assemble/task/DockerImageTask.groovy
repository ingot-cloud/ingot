package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.extension.DockerExtension
import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
/**
 * <p>Description  : DockerImageTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:23 AM.</p>
 */
class DockerImageTask extends DefaultTask {

    DockerImageTask() {
        setGroup("ingot")
        setDescription("Ingot make docker image")
    }

    @TaskAction
    dockerImage() {
        AssembleExtension assembleExtension = AssembleExtension.getBuildExtension(project)
        DockerExtension dockerExtension = assembleExtension.docker

        String outputDirPath = assembleExtension.outputPath

        if (Utils.isEmpty(outputDirPath)) {
            outputDirPath = Utils.defaultOutputDirPath(project)
        }

        // build dockerfile 的目录
        String buildDirPath = Utils.projectOutputPath(outputDirPath, project)

        if (!new File(buildDirPath).exists()) {
            throw new GradleException("不存在执行文件夹")
        }

        String name = project.name + ":" + project.version
        name = String.join("/", name.split("-", 2))

//        // remove origin if exist
//        try {
//            CommandUtils.executeAndWait(["docker", "rmi", name])
//        } catch (Exception e) {
//            project.logger.error("> ingore exception", e)
//        }
//
//        // build new image
//        def cmd = ["docker", "build", "-t", name, buildDirPath]
//        CommandUtils.executeAndWait(cmd)
    }
}
