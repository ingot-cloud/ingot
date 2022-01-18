package com.ingot.plugin.assemble.task


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
    /**
     * docker registry
     */
    private String registry
    /**
     * 工程打包输出目录路径
     */
    private String outputDirPath
    /**
     * docker命令path
     */
    private String dockerCmd
    /**
     * docker文件目录path
     */
    private String dockerfileDir
    /**
     * build name
     */
    private String name

    DockerBuildTask() {
        setGroup("ingot")
    }

    @TaskAction
    dockerImage() {
        dockerfileDir = Utils.getDockerFileDirPathOrDefault(project, dockerfileDir)
        outputDirPath = Utils.getOutputDirPathOrDefault(project, outputDirPath)
        dockerCmd = Utils.getDockerCmdOrDefault(dockerCmd)

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
        project.logger.lifecycle(">>> dockerfile copyTo: " + buildDirPath)

        String tag = Utils.getTag(project, name, registry)

        project.exec {
            workingDir buildDirPath
            commandLine dockerCmd, 'build', '-t', tag, '.'
            logging.captureStandardOutput LogLevel.INFO
            logging.captureStandardError LogLevel.ERROR
        }

        project.logger.lifecycle(">>> docker build success, tag=" + tag)

    }

    String getRegistry() {
        return registry
    }

    String getOutputDirPath() {
        return outputDirPath
    }

    String getDockerCmd() {
        return dockerCmd
    }

    String getDockerfileDir() {
        return dockerfileDir
    }

    String getName() {
        return name
    }

    void setRegistry(String registry) {
        this.registry = registry
    }

    void setOutputDirPath(String outputDirPath) {
        this.outputDirPath = outputDirPath
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
    }

    void setDockerfileDir(String dockerfileDir) {
        this.dockerfileDir = dockerfileDir
    }

    void setName(String name) {
        this.name = name
    }
}
