package com.ingot.plugin.assemble.task


import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
/**
 * <p>Description  : AssembleTask.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 9:37 AM.</p>
 */
class AssembleTask extends DefaultTask {

    /**
     * 工程打包输出目录路径
     */
    @Internal
    private String outputDirPath

    AssembleTask() {
        setGroup("ingot")
        setDescription("Ingot assemble")
    }

    @TaskAction
    void assembleRelease() {
        project.logger.lifecycle("Assemble release task running.")
        outputDirPath = Utils.getOutputDirPathOrDefault(project, outputDirPath)

        // assemble 生成的 jar 包文件夹
        String libsPath = project.buildDir.path + "/libs"
        // 输出文件目录
        String outputPath = Utils.projectOutputPath(outputDirPath, project)
        // 输出文件名字
        String outputFileName = project.name + ".jar"
        project.logger.lifecycle(">>> copy to: " + outputPath)

        // copy jar
        project.copy {
            duplicatesStrategy DuplicatesStrategy.INCLUDE
            from(libsPath){
                include "${project.name}-${project.version}.jar"
            }
            into outputPath
            rename {
                outputFileName
            }
        }

    }

    String getOutputDirPath() {
        return outputDirPath
    }

    void setOutputDirPath(String outputDirPath) {
        this.outputDirPath = outputDirPath
    }
}
