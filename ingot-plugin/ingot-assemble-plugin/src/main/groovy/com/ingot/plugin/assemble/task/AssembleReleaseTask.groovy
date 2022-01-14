package com.ingot.plugin.assemble.task

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * <p>Description  : AssembleReleaseTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:37 AM.</p>
 */
class AssembleReleaseTask extends DefaultTask {

    AssembleReleaseTask() {
        setGroup("ingot")
        setDescription("Ingot assemble")
    }

    @TaskAction
    void assembleRelease() {
        project.logger.lifecycle("Assemble release task running.")

        AssembleExtension assembleExtension = AssembleExtension.getBuildExtension(project)
        String outputDirPath = assembleExtension.outputDirPath

        if (Utils.isEmpty(outputDirPath)) {
            outputDirPath = Utils.defaultOutputDirPath(project)
        }

        // assemble 生成的 jar 包文件夹
        String libsPath = project.buildDir.path + "/libs"
        // 输出文件目录
        String outputPath = Utils.projectOutputPath(outputDirPath, project)
        // 输出文件名字
        String outputFileName = project.name + ".jar"
        project.logger.lifecycle(">>> copy to: " + outputPath)

        // copy jar
        project.copy {
            from libsPath
            into outputPath
            rename {
                outputFileName
            }
        }

    }
}
