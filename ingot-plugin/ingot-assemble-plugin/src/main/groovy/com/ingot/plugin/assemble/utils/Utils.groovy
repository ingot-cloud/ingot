package com.ingot.plugin.assemble.utils


import org.gradle.api.Project

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 1:44 PM.</p>
 */
class Utils {

    static boolean isEmpty(String str) {
        return str == null || str.length() == 0
    }

    static boolean isNotEmpty(String str) {
        return !isEmpty(str)
    }

    static String getOutputDirPathOrDefault(Project project, String target) {
        if (isEmpty(target)) {
            return defaultOutputDirPath(project)
        }
        return target
    }

    static String getDockerFileDirPathOrDefault(Project project, String target) {
        if (isEmpty(target)) {
            return defaultDockerFileDirPath(project)
        }
        return target
    }

    static String getDockerCmdOrDefault(String target) {
        if (isEmpty(target)) {
            return "docker"
        }
        return target
    }

    /**
     * 默认的输出目录地址
     * @param project
     * @return
     */
    static String defaultOutputDirPath(Project project) {
        return project.rootProject.projectDir.path + "/output"
    }

    /**
     * 默认的dockerfile存储目录
     * @param project
     * @return
     */
    static String defaultDockerFileDirPath(Project project) {
        return project.projectDir.path + "/src/main/docker"
    }

    /**
     * 项目输出目录，相对于outputDirPath
     * @param outputDirPath
     * @param project
     * @return
     */
    static String projectOutputPath(String outputDirPath, Project project) {
        return outputDirPath + "/" + project.name + "/" + project.version
    }

    /**
     * 获取Tag
     * @param project
     * @param name
     * @param registry
     * @return
     */
    static String getTag(Project project, String name, String registry) {
        if (isEmpty(name)) {
            name = project.name
            name = String.join("/", name.split("-", 2))
        }
        String tag = name + ":" + project.version
        if (!isEmpty(registry)) {
            tag = registry + "/" + tag
        }
        return tag
    }
}
