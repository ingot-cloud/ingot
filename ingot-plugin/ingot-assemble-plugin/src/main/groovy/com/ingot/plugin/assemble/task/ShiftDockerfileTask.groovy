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
     * docker文件目录path（相对于 src/main/docker 的路径）
     */
    @Internal
    private String dockerfileDir
    /**
     * 环境标签名称（如 dev、test、prod）
     */
    @Internal
    private String tagName

    ShiftDockerfileTask() {
        setGroup("ingot")
    }

    @TaskAction
    dockerImage() {
        project.logger.lifecycle("ShiftDockerfileTask running")
        
        // 解析最终的 Dockerfile 目录
        String finalDockerfileDir = resolveDockerfileDir()
        outputDirPath = Utils.getOutputDirPathOrDefault(project, outputDirPath)

        // build dockerfile 的目录
        String buildDirPath = Utils.projectOutputPath(outputDirPath, project)

        if (!new File(buildDirPath).exists()) {
            throw new GradleException("构建输出目录不存在: ${buildDirPath}")
        }

        File dockerfileDirFile = new File(finalDockerfileDir)
        if (!dockerfileDirFile.exists()) {
            throw new GradleException("Dockerfile 目录不存在: ${finalDockerfileDir}")
        }

        project.logger.lifecycle("ShiftDockerfileTask - dockerfile dir: ${finalDockerfileDir}")
        project.logger.lifecycle("ShiftDockerfileTask - target dir: ${buildDirPath}")
        
        // copy dockerfile 目录中所有文件
        project.copy {
            duplicatesStrategy DuplicatesStrategy.INCLUDE
            from finalDockerfileDir
            into buildDirPath
        }
        
        project.logger.lifecycle("ShiftDockerfileTask - copy completed")
    }

    /**
     * 解析最终的 Dockerfile 目录
     * 优先级：
     * 1. 如果指定了 dockerfileDir，则使用它（相对于 src/main/docker）
     * 2. 如果指定了 tagName，则使用 src/main/docker/{tagName}
     * 3. 否则使用默认的 src/main/docker
     */
    private String resolveDockerfileDir() {
        String baseDockerDir = project.projectDir.path + "/src/main/docker"
        
        if (Utils.isNotEmpty(dockerfileDir)) {
            // 用户指定了 dockerfileDir
            // 如果是绝对路径，直接使用；否则相对于 baseDockerDir
            File dirFile = new File(dockerfileDir)
            if (dirFile.isAbsolute()) {
                return dockerfileDir
            } else {
                return baseDockerDir + "/" + dockerfileDir
            }
        } else if (Utils.isNotEmpty(tagName)) {
            // 使用 tagName 作为子目录
            return baseDockerDir + "/" + tagName
        } else {
            // 使用默认目录
            return baseDockerDir
        }
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

    String getTagName() {
        return tagName
    }

    void setTagName(String tagName) {
        this.tagName = tagName
    }
}
