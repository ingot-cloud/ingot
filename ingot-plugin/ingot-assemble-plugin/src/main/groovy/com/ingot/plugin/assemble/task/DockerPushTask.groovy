package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.extension.DockerExtension
import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

/**
 * <p>Description  : DockerPushTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/14.</p>
 * <p>Time         : 1:06 下午.</p>
 */
class DockerPushTask extends DefaultTask {

    DockerPushTask() {
        setGroup("ingot")
        setDescription("Push docker image")
    }

    @TaskAction
    push() {
        AssembleExtension assembleExtension = AssembleExtension.getBuildExtension(project)
        DockerExtension dockerExtension = assembleExtension.docker

        String dockerCmd = dockerExtension.dockerCmd
        String username = dockerExtension.username
        String password = dockerExtension.password
        String registry = dockerExtension.registry

        if (Utils.isEmpty(dockerCmd)) {
            dockerCmd = "docker"
        }

        if (Utils.isEmpty(registry)) {
            throw new GradleException("注册中心地址不能为空")
        }

        String tag = Utils.getTag(dockerExtension, project)

        // login
        if (!Utils.isEmpty(username) && !Utils.isEmpty(password)) {
            project.logger.lifecycle(">>> docker login")
            project.exec {
                commandLine dockerCmd, "login", "-u", username, "-p", password, registry
                setStandardOutput System.in
                setErrorOutput System.err
                logging.captureStandardOutput LogLevel.INFO
                logging.captureStandardError LogLevel.ERROR
            }
            project.logger.lifecycle(">>> docker login success")
        }

        project.logger.lifecycle(">>> docker image push: " + tag)
        project.exec {
            commandLine dockerCmd, 'push', tag
            setStandardOutput System.in
            setErrorOutput System.err
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }
        project.logger.lifecycle(">>> docker image push success")

    }
}
