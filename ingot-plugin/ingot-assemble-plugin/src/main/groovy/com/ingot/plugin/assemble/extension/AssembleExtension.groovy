package com.ingot.plugin.assemble.extension

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

/**
 * <p>Description  : AssembleExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 10:32 AM.</p>
 */
class AssembleExtension {

    /**
     * 输出目录路径
     */
    String outputDirPath

    /**
     * docker配置
     */
    DockerExtension docker

    AssembleExtension(ObjectFactory objectFactory) {
        docker = objectFactory.newInstance(DockerExtension)
    }

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
