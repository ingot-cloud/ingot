package com.ingot.plugin.assemble.utils


import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecOperations

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/12.</p>
 * <p>Time         : 1:44 PM.</p>
 */
class Utils {

    private static final String DEFAULT_DOCKER_CMD = "docker"

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
            return DEFAULT_DOCKER_CMD
        }
        return target
    }

    /**
     * 解析 Docker CLI 路径，优先级：显式配置 &gt; DOCKER_CMD 环境变量 &gt; ingot.dockerCmd 属性 &gt; 常见安装路径 &gt; docker
     */
    static String resolveDockerCmd(Project project, String configured) {
        if (isNotEmpty(configured) && configured != DEFAULT_DOCKER_CMD) {
            return configured
        }

        String fromEnv = System.getenv("DOCKER_CMD")
        if (isNotEmpty(fromEnv)) {
            return fromEnv
        }

        def fromProperty = project.findProperty("ingot.dockerCmd")
        if (fromProperty != null && isNotEmpty(fromProperty.toString())) {
            return fromProperty.toString()
        }

        String detected = detectDockerCmd()
        if (detected != null) {
            return detected
        }

        return DEFAULT_DOCKER_CMD
    }

    static void requireDockerCmd(String dockerCmd) {
        if (DEFAULT_DOCKER_CMD == dockerCmd) {
            return
        }
        if (!isDockerExecutable(new File(dockerCmd))) {
            throw new GradleException(buildDockerNotFoundMessage(dockerCmd))
        }
    }

    static void execDocker(ExecOperations execOperations, Project project, String dockerCmd, Closure execSpec) {
        requireDockerCmd(dockerCmd)
        try {
            execOperations.exec(execSpec)
        } catch (Exception ex) {
            throw new GradleException(buildDockerExecFailureMessage(dockerCmd, ex), ex)
        }
    }

    private static String detectDockerCmd() {
        List<String> candidates = dockerCmdCandidates()
        for (String candidate : candidates) {
            if (isDockerExecutable(new File(candidate))) {
                return candidate
            }
        }
        return null
    }

    private static List<String> dockerCmdCandidates() {
        List<String> candidates = []
        String osName = System.getProperty("os.name", "").toLowerCase()
        if (osName.contains("mac")) {
            candidates.addAll([
                    "/usr/local/bin/docker",
                    "/opt/homebrew/bin/docker",
                    "/Applications/Docker.app/Contents/Resources/bin/docker"
            ])
        } else if (osName.contains("linux")) {
            candidates.addAll([
                    "/usr/bin/docker",
                    "/usr/local/bin/docker"
            ])
        } else if (osName.contains("windows")) {
            String programFiles = System.getenv("ProgramFiles")
            if (isNotEmpty(programFiles)) {
                candidates.add("${programFiles}\\Docker\\Docker\\resources\\bin\\docker.exe")
            }
            String programFilesX86 = System.getenv("ProgramFiles(x86)")
            if (isNotEmpty(programFilesX86)) {
                candidates.add("${programFilesX86}\\Docker\\Docker\\resources\\bin\\docker.exe")
            }
            String localAppData = System.getenv("LOCALAPPDATA")
            if (isNotEmpty(localAppData)) {
                candidates.add("${localAppData}\\Docker\\resources\\bin\\docker.exe")
            }
        }
        return candidates
    }

    private static boolean isDockerExecutable(File dockerFile) {
        if (!dockerFile.isFile()) {
            return false
        }
        if (isWindows()) {
            return dockerFile.exists()
        }
        return dockerFile.canExecute()
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows")
    }

    private static String buildDockerNotFoundMessage(String dockerCmd) {
        return """Docker 命令不可用: ${dockerCmd}
请确认已安装 Docker Desktop 且可执行文件存在，或在 gradle.properties 中设置:
  macOS/Linux: ingot.dockerCmd=/usr/local/bin/docker
  Windows:     ingot.dockerCmd=C:\\\\Program Files\\\\Docker\\\\Docker\\\\resources\\\\bin\\\\docker.exe
也可通过环境变量 DOCKER_CMD 指定路径。"""
    }

    private static String buildDockerExecFailureMessage(String dockerCmd, Exception cause) {
        if (DEFAULT_DOCKER_CMD == dockerCmd) {
            return """无法执行 Docker 命令 '${dockerCmd}'。
常见原因：IDE 中 Gradle Daemon 的 PATH 不包含 docker 可执行文件。
请尝试：
  1. 确认 Docker Desktop 已安装并正在运行
  2. 在 gradle.properties 设置 ingot.dockerCmd
     macOS:   /usr/local/bin/docker
     Windows: C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe
  3. 或设置环境变量 DOCKER_CMD
  4. 执行 ./gradlew --stop 后重试"""
        }
        return """无法执行 Docker 命令 '${dockerCmd}'。
请确认 Docker Desktop 已安装并正在运行，或检查 ingot.dockerCmd / DOCKER_CMD 配置是否正确。
原始错误: ${cause.message}"""
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
     * 不再包含版本号，避免每次构建都创建新目录
     * @param outputDirPath
     * @param project
     * @return
     */
    static String projectOutputPath(String outputDirPath, Project project) {
        return outputDirPath + "/" + project.name
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
