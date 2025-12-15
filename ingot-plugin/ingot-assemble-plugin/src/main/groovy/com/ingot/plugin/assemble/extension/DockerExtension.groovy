package com.ingot.plugin.assemble.extension

import com.ingot.plugin.assemble.utils.DockerPlatformEnums

/**
 * <p>Description  : DockerExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/11.</p>
 * <p>Time         : 11:25 上午.</p>
 */
class DockerExtension {

    /**
     * docker命令path
     */
    String dockerCmd = "docker"

    /**
     * 默认编译平台 linux/amd64
     * linux/amd64,linux/arm64
     */
    String platform = DockerPlatformEnums.LINUX_AMD64.value

    /**
     * 注册中心地址
     */
    String registry = ""

    /**
     * 注册中心登录用户名
     */
    String username = ""

    /**
     * 注册中心登录密码
     */
    String password = ""

    /**
     * dockerfile 存储目录（默认目录，通常不需要设置）
     */
    String dockerfileDir = ""

    /**
     * 环境配置（必填）
     */
    Map<String, Env> envs = new HashMap<>()

    DockerExtension() {}

    String getDockerCmd() {
        return dockerCmd
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
    }

    String getPlatform() {
        return platform
    }

    void setPlatform(String platform) {
        this.platform = platform
    }

    String getRegistry() {
        return registry
    }

    void setRegistry(String registry) {
        this.registry = registry
    }

    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    String getDockerfileDir() {
        return dockerfileDir
    }

    void setDockerfileDir(String dockerfileDir) {
        this.dockerfileDir = dockerfileDir
    }

    Map<String, Env> getEnvs() {
        return envs
    }

    void setEnvs(Map<String, Env> envs) {
        this.envs = envs
    }

    /**
     * 添加单个环境配置（支持 Gradle DSL 风格）
     * @param envName 环境名称（如 dev、test、prod）
     * @param action 环境配置闭包
     */
    void env(String envName, Closure action) {
        Env env = new Env()
        action.delegate = env
        action.resolveStrategy = Closure.DELEGATE_FIRST
        action.call()
        envs.put(envName, env)
    }
}
