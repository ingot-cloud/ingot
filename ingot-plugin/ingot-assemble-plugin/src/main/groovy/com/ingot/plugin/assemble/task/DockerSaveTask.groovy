package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * <p>Description  : DockerSaveTask.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/10/25.</p>
 * <p>Time         : 15:21.</p>
 */
class DockerSaveTask extends DefaultTask {
    /**
     * docker registry
     */
    @Internal
    private String registry
    /**
     * 工程打包输出目录路径
     */
    @Internal
    private String outputDirPath
    /**
     * docker命令path
     */
    @Internal
    private String dockerCmd
    /**
     * build name
     */
    @Internal
    private String imageName
    /**
     * 保存文件名称
     */
    @Internal
    private String saveName

    DockerSaveTask() {
        setGroup("ingot")
    }

    @TaskAction
    dockerSave() {
        project.logger.lifecycle("DockerSaveTask running.")
        outputDirPath = Utils.getOutputDirPathOrDefault(project, outputDirPath)
        dockerCmd = Utils.getDockerCmdOrDefault(dockerCmd)

        // build dockerfile 的目录
        String buildDirPath = Utils.projectOutputPath(outputDirPath, project)
        String tag = Utils.getTag(project, imageName, registry)

        project.logger.lifecycle(dockerCmd + " save -o " + saveName + " " + tag)

        project.exec {
            workingDir buildDirPath
            commandLine dockerCmd, 'save', '-o', saveName, tag
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }

        project.logger.lifecycle("DockerSaveTask - docker save success, tag=" + tag + ", save name=" + saveName)
    }

    String getRegistry() {
        return registry
    }

    void setRegistry(String registry) {
        this.registry = registry
    }

    String getOutputDirPath() {
        return outputDirPath
    }

    void setOutputDirPath(String outputDirPath) {
        this.outputDirPath = outputDirPath
    }

    String getDockerCmd() {
        return dockerCmd
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
    }

    String getImageName() {
        return imageName
    }

    void setImageName(String imageName) {
        this.imageName = imageName
    }

    String getSaveName() {
        return saveName
    }

    void setSaveName(String saveName) {
        this.saveName = saveName
    }
}
