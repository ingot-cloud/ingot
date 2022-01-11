package com.ingot.plugin.assemble.extension

import org.gradle.api.Action
import org.gradle.api.Project

/**
 * <p>Description  : AssembleExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 10:32 AM.</p>
 */
class AssembleExtension {

    /**
     * dockerfile 路径
     */
    String dockerFilePath

    /**
     * 输出目录路径
     */
    String outputPath

    /**
     * docker配置
     */
    DockerExtension docker = new DockerExtension()

    AssembleExtension() {}

    void docker(Action<DockerExtension> action) {
        action.execute(this.docker)
    }

    static AssembleExtension getBuildExtension(Project project) {
        AssembleExtension assembleExtension = project.getExtensions().findByType(AssembleExtension)
        if (assembleExtension == null) {
            assembleExtension = new AssembleExtension()
        }

        return assembleExtension
    }
}
