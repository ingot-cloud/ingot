package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.extension.DockerExtension
import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

/**
 * <p>Description  : DockerBuildTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:23 AM.</p>
 */
class DockerBuildTask extends DefaultTask {

    DockerBuildTask() {
        setGroup("ingot")
        setDescription("Ingot make docker image")
    }

    @TaskAction
    dockerImage() {
        AssembleExtension assembleExtension = AssembleExtension.getBuildExtension(project)
        DockerExtension dockerExtension = assembleExtension.docker

        String outputDirPath = assembleExtension.outputDirPath
        String dockerfileDir = dockerExtension.dockerfileDir
        String dockerCmd = dockerExtension.dockerCmd

        if (Utils.isEmpty(dockerfileDir)) {
            dockerfileDir = Utils.defaultDockerFileDirPath(project)
        }

        if (Utils.isEmpty(outputDirPath)) {
            outputDirPath = Utils.defaultOutputDirPath(project)
        }

        if (Utils.isEmpty(dockerCmd)) {
            dockerCmd = "docker"
        }

        // build dockerfile 的目录
        String buildDirPath = Utils.projectOutputPath(outputDirPath, project)

        if (!new File(buildDirPath).exists()) {
            throw new GradleException("不存在执行文件夹")
        }

        project.logger.lifecycle(">>> dockerfile dir: " + dockerfileDir)
        // copy dockerfile 已经目录中所有文件
        project.copy {
            duplicatesStrategy DuplicatesStrategy.INCLUDE
            from dockerfileDir
            into buildDirPath
        }
        project.logger.lifecycle(">>> dockerfile copyTo: " + outputDirPath)

        String tag = Utils.getTag(dockerExtension, project)

        project.exec {
            workingDir buildDirPath
            commandLine dockerCmd, 'build', '-t', tag, '.'
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }

        project.logger.lifecycle(">>> docker build success, tag=" + tag)

    }
}
