package com.ingot.plugin.assemble

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.extension.DockerExtension
import com.ingot.plugin.assemble.extension.Env
import com.ingot.plugin.assemble.task.*
import com.ingot.plugin.assemble.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>Description  : BulidPlugin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/11.</p>
 * <p>Time         : 5:11 PM.</p>
 */
class AssemblePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // create extension
        AssembleExtension ext = project.extensions.create("ingotAssemble", AssembleExtension, project.objects)

        project.afterEvaluate {
            // clean task
            project.tasks.register("cleanAll", CleanTask)

            // build task
            project.tasks.register("ingotAssemble", AssembleTask) {
                dependsOn project.tasks.named("assemble")
                outputDirPath = ext.getOutputDirPath()
            }

            DockerExtension dockerExtension = ext.docker
            
            // docker task - 环境配置
            Map<String, Env> envs = dockerExtension.envs
            if (envs == null || envs.isEmpty()) {
                project.logger.warn("未配置 Docker 环境（envs），跳过 Docker 任务创建")
                project.logger.warn("请在 ingotAssemble.docker 中配置至少一个环境")
                return
            }
            
            envs.each { envName, env ->
                createDockerTask(project, ext, envName, env)
            }
        }
    }

    /**
     * 创建 Docker 相关任务
     * @param project Gradle Project
     * @param ext AssembleExtension 配置
     * @param envName 环境名称（如 "dev"、"test"、"prod"）
     * @param env 环境配置
     */
    static void createDockerTask(Project project,
                                 AssembleExtension ext,
                                 String envName,
                                 Env env) {
        DockerExtension dockerExtension = ext.docker

        // 验证环境配置
        if (Utils.isEmpty(env?.name)) {
            project.logger.error("环境 '${envName}' 未配置镜像名称（name），跳过该环境的任务创建")
            return
        }

        // 获取配置：优先使用 Env 的配置，否则使用 DockerExtension 的全局配置
        String imageName = env.name  // 环境必须配置自己的镜像名称
        String dockerfileDir = getConfig(env.dockerfileDir, dockerExtension.dockerfileDir)
        String registry = getConfig(env.registry, dockerExtension.registry)
        String platform = getConfig(env.platform, dockerExtension.platform)
        String dockerCmd = getConfig(env.dockerCmd, dockerExtension.dockerCmd)
        String username = getConfig(env.username, dockerExtension.username)
        String password = getConfig(env.password, dockerExtension.password)
        String saveName = env.saveName  // 保存文件名不使用全局配置

        String[] names = envName.split("/")
        String finalSuffix = names.collect { name ->
            name.capitalize()
        }.join("")

        project.logger.lifecycle("Creating Docker tasks for environment '${envName}' - image: ${imageName}, registry: ${registry}")

        project.tasks.register("shiftDockerfile${finalSuffix}", ShiftDockerfileTask) {
            description = "Copy Dockerfile for ${envName} environment"
            outputDirPath = ext.getOutputDirPath()
            it.dockerfileDir = dockerfileDir
            it.tagName = envName  // 传递环境名称，用于智能解析 Dockerfile 目录
        }

        project.tasks.register("dockerBuild${finalSuffix}", DockerBuildTask) {
            dependsOn project.tasks.named("shiftDockerfile${finalSuffix}")
            description = "Build Docker image '${imageName}' for ${envName} environment"
            it.registry = registry
            outputDirPath = ext.getOutputDirPath()
            it.dockerCmd = dockerCmd
            it.imageName = imageName
            it.platform = platform
        }

        project.tasks.register("dockerPush${finalSuffix}", DockerPushTask) {
            dependsOn project.tasks.named("dockerBuild${finalSuffix}")
            description = "Push Docker image '${imageName}' to registry for ${envName} environment"
            it.registry = registry
            it.username = username
            it.password = password
            it.dockerCmd = dockerCmd
            it.imageName = imageName
        }

        project.tasks.register("dockerSave${finalSuffix}", DockerSaveTask) {
            dependsOn project.tasks.named("dockerBuild${finalSuffix}")
            description = "Save Docker image '${imageName}' as tar file for ${envName} environment"
            it.registry = registry
            outputDirPath = ext.getOutputDirPath()
            it.dockerCmd = dockerCmd
            it.imageName = imageName
            it.saveName = saveName
        }
    }

    /**
     * 获取配置值：优先使用 Tag 的配置，如果为空则使用默认配置
     * @param tagValue Tag 中的配置值
     * @param defaultValue 默认配置值
     * @return 最终使用的配置值
     */
    private static String getConfig(String tagValue, String defaultValue) {
        return Utils.isNotEmpty(tagValue) ? tagValue : defaultValue
    }
}
