package com.ingot.plugin.assemble.extension

import lombok.Data

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
     * dockerfile 存储目录
     */
    String dockerfileDir = ""

    /**
     * 存储库名称
     */
    String name = ""

    /**
     * 多个Tag打包
     */
    Map<String, Tag> tags = new HashMap<>()

    DockerExtension() {}

    String getDockerCmd() {
        return dockerCmd
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
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

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    Map<String, Tag> getTags() {
        return tags
    }

    void setTags(Map<String, Tag> tags) {
        this.tags = tags
    }
}
