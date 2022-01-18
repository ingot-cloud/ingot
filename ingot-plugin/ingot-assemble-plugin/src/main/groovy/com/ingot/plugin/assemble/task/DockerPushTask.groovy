package com.ingot.plugin.assemble.task


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
    /**
     * docker registry
     */
    private String registry
    /**
     * docker命令path
     */
    private String dockerCmd
    /**
     * docker registry username，如果需要登录则必填
     */
    private String username
    /**
     * docker registry password，如果需要登录则必填
     */
    private String password
    /**
     * push name
     */
    private String name

    DockerPushTask() {
        setGroup("ingot")
    }

    @TaskAction
    push() {
        dockerCmd = Utils.getDockerCmdOrDefault(dockerCmd)

        if (Utils.isEmpty(registry)) {
            throw new GradleException("注册中心地址不能为空")
        }

        String tag = Utils.getTag(project, name, registry)

        // login
        if (!Utils.isEmpty(username) && !Utils.isEmpty(password)) {
            project.logger.lifecycle(">>> docker login")
            project.exec {
                commandLine dockerCmd, "login", "-u", username, "-p", password, registry
                logging.captureStandardOutput LogLevel.INFO
                logging.captureStandardError LogLevel.ERROR
            }
            project.logger.lifecycle(">>> docker login success")
        }

        project.logger.lifecycle(">>> docker image push: " + tag)
        project.exec {
            commandLine dockerCmd, 'push', tag
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }
        project.logger.lifecycle(">>> docker image push success")

    }

    void setRegistry(String registry) {
        this.registry = registry
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
    }

    void setUsername(String username) {
        this.username = username
    }

    void setPassword(String password) {
        this.password = password
    }

    void setName(String name) {
        this.name = name
    }
}
