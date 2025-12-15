package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

/**
 * <p>Description  : DockerPushTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/14.</p>
 * <p>Time         : 1:06 下午.</p>
 */
class DockerPushTask extends DefaultTask {

    private final ExecOperations execOperations

    /**
     * docker registry
     */
    @Internal
    private String registry
    /**
     * docker命令path
     */
    @Internal
    private String dockerCmd
    /**
     * docker registry username，如果需要登录则必填
     */
    @Internal
    private String username
    /**
     * docker registry password，如果需要登录则必填
     */
    @Internal
    private String password
    /**
     * push name
     */
    @Internal
    private String imageName

    @Inject
    DockerPushTask(ExecOperations execOperations) {
        this.execOperations = execOperations
        setGroup("ingot")
    }

    @TaskAction
    push() {
        project.logger.lifecycle("DockerPushTask running")

        dockerCmd = Utils.getDockerCmdOrDefault(dockerCmd)

        if (Utils.isEmpty(registry)) {
            throw new GradleException("注册中心地址不能为空")
        }

        String tag = Utils.getTag(project, imageName, registry)

        // login
        if (!Utils.isEmpty(username) && !Utils.isEmpty(password)) {
            project.logger.lifecycle("ShiftDockerfileTask - docker login")
            execOperations.exec {
                commandLine dockerCmd, "login", "-u", username, "-p", password, registry
                logging.captureStandardOutput LogLevel.INFO
                logging.captureStandardError LogLevel.ERROR
            }
            project.logger.lifecycle("DockerPushTask - docker login success")
        }

        project.logger.lifecycle("DockerPushTask - docker image push: " + tag)
        execOperations.exec {
            commandLine dockerCmd, 'push', tag
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }
        project.logger.lifecycle("DockerPushTask - docker image push success")

    }

    String getRegistry() {
        return registry
    }

    void setRegistry(String registry) {
        this.registry = registry
    }

    String getDockerCmd() {
        return dockerCmd
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
    }

    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    String getImageName() {
        return imageName
    }

    void setImageName(String imageName) {
        this.imageName = imageName
    }
}
