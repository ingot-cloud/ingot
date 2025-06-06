package com.ingot.plugin.assemble

import com.ingot.plugin.assemble.extension.AssembleExtension
import com.ingot.plugin.assemble.extension.DockerExtension
import com.ingot.plugin.assemble.extension.Tag
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
            project.tasks.create("cleanAll", CleanTask)

            // build task
            project.tasks.create("ingotAssemble", AssembleTask, {
                dependsOn project.tasks.getByName("assemble")
                outputDirPath = ext.getOutputDirPath()
            })

            DockerExtension dockerExtension = ext.docker
            // docker task
            if (Utils.isNotEmpty(dockerExtension.name)) {
                createDockerTask(project, ext, dockerExtension.name, dockerExtension.name, dockerExtension.dockerfileDir)
            }

            Map<String, Tag> tags = dockerExtension.tags
            if (tags != null && !tags.isEmpty()) {
                tags.each { tagName, item ->
                    createDockerTask(project, ext, tagName, item.name, item.dockerfileDir)
                }
            }
        }
    }

    static void createDockerTask(Project project,
                                 AssembleExtension ext,
                                 String taskNameSuffix,
                                 String inputImageName,
                                 String dockerfileDirPath) {
        DockerExtension dockerExtension = ext.docker

        String[] names = taskNameSuffix.split("/")
        String finalSuffix = names.collect { name ->
            name.capitalize()
        }.join("")

        project.logger.lifecycle("registry:" + dockerExtension.getRegistry())

        project.tasks.create("shiftDockerfile${finalSuffix}", ShiftDockerfileTask, {
            description = "Shift dockerfile"
            outputDirPath = ext.getOutputDirPath()
            dockerfileDir = dockerfileDirPath
        })

        project.tasks.create("dockerBuild${finalSuffix}", DockerBuildTask, {
            dependsOn project.tasks.getByName("shiftDockerfile${finalSuffix}")
            description = "Docker image with name '${inputImageName}'"
            registry = dockerExtension.getRegistry()
            outputDirPath = ext.getOutputDirPath()
            dockerCmd = dockerExtension.getDockerCmd()
            imageName = inputImageName
            platform = dockerExtension.getPlatform()
        })

        project.tasks.create("dockerPush${finalSuffix}", DockerPushTask, {
            dependsOn project.tasks.getByName("dockerBuild${finalSuffix}")
            description = "Push the docker image named '${inputImageName}'"
            registry = dockerExtension.getRegistry()
            username = dockerExtension.getUsername()
            password = dockerExtension.getPassword()
            dockerCmd = dockerExtension.getDockerCmd()
            imageName = inputImageName
        })

        project.tasks.create("dockerSave${finalSuffix}", DockerSaveTask, {
            dependsOn project.tasks.getByName("dockerBuild${finalSuffix}")
            description = "Save the docker image named '${inputImageName}'"
            registry = dockerExtension.getRegistry()
            outputDirPath = ext.getOutputDirPath()
            dockerCmd = dockerExtension.getDockerCmd()
            imageName = inputImageName
            saveName = dockerExtension.getSaveName()
        })
    }
}
