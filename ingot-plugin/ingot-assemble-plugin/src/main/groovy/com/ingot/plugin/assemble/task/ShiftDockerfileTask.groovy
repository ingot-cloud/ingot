package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
/**
 * <p>Description  : ShiftDockerfileTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/2.</p>
 * <p>Time         : 12:14 PM.</p>
 */
class ShiftDockerfileTask extends DefaultTask {
    @Internal
    private String outputDirPath
    /**
     * docker文件目录path
     */
    @Internal
    private String dockerfileDir

    ShiftDockerfileTask() {
        setGroup("ingot")
    }

    @TaskAction
    dockerImage() {
        project.logger.lifecycle("ShiftDockerfileTask running")
        dockerfileDir = Utils.getDockerFileDirPathOrDefault(project, dockerfileDir)
        outputDirPath = Utils.getOutputDirPathOrDefault(project, outputDirPath)

        // build dockerfile 的目录
        String buildDirPath = Utils.projectOutputPath(outputDirPath, project)

        if (!new File(buildDirPath).exists()) {
            throw new GradleException("不存在执行文件夹")
        }

        project.logger.lifecycle("ShiftDockerfileTask - dockerfile dir: " + dockerfileDir)
        // copy dockerfile 目录中所有文件
        project.copy {
            duplicatesStrategy DuplicatesStrategy.INCLUDE
            from dockerfileDir
            into buildDirPath
        }
        project.logger.lifecycle("ShiftDockerfileTask - dockerfile copyTo: " + buildDirPath)
    }

    String getOutputDirPath() {
        return outputDirPath
    }

    void setOutputDirPath(String outputDirPath) {
        this.outputDirPath = outputDirPath
    }

    String getDockerfileDir() {
        return dockerfileDir
    }

    void setDockerfileDir(String dockerfileDir) {
        this.dockerfileDir = dockerfileDir
    }
}
