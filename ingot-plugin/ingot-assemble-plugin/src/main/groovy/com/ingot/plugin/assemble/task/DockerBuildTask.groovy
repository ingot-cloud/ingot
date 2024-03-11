package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
/**
 * <p>Description  : DockerBuildTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:23 AM.</p>
 */
class DockerBuildTask extends DefaultTask {
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

    @Internal
    private String platform

    DockerBuildTask() {
        setGroup("ingot")
    }

    @TaskAction
    dockerImage() {
        project.logger.lifecycle("DockerBuildTask running.")
        outputDirPath = Utils.getOutputDirPathOrDefault(project, outputDirPath)
        dockerCmd = Utils.getDockerCmdOrDefault(dockerCmd)

        // build dockerfile 的目录
        String buildDirPath = Utils.projectOutputPath(outputDirPath, project)
        String tag = Utils.getTag(project, imageName, registry)

        project.logger.lifecycle(dockerCmd + " buildx build --platform " + platform + " -t " + tag + " .")

        project.exec {
            workingDir buildDirPath
            commandLine dockerCmd, 'buildx', 'build', '--platform', platform, '-t', tag, '.'
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }

        project.logger.lifecycle("DockerBuildTask - docker build success, tag=" + tag)

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

    String getPlatform() {
        return platform
    }

    void setPlatform(String platform) {
        this.platform = platform
    }
}
