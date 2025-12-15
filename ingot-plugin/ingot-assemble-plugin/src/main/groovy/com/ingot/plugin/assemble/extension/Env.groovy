package com.ingot.plugin.assemble.extension

/**
 * <p>Description  : Env - Docker 构建环境配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/18.</p>
 * <p>Time         : 11:26 上午.</p>
 */
class Env {
    /**
     * 镜像名称（必填）
     */
    private String name

    /**
     * dockerfile 存储目录（可选，不设置则自动使用 src/main/docker/{envName}）
     */
    private String dockerfileDir

    /**
     * 注册中心地址（可选，不设置则使用全局配置）
     */
    private String registry

    /**
     * 编译平台（可选，不设置则使用全局配置）
     * 例如：linux/amd64 或 linux/amd64,linux/arm64
     */
    private String platform

    /**
     * 保存镜像文件名（可选）
     */
    private String saveName

    /**
     * 注册中心登录用户名（可选，不设置则使用全局配置）
     */
    private String username

    /**
     * 注册中心登录密码（可选，不设置则使用全局配置）
     */
    private String password

    /**
     * docker命令path（可选，不设置则使用全局配置）
     */
    private String dockerCmd

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getDockerfileDir() {
        return dockerfileDir
    }

    void setDockerfileDir(String dockerfileDir) {
        this.dockerfileDir = dockerfileDir
    }

    String getRegistry() {
        return registry
    }

    void setRegistry(String registry) {
        this.registry = registry
    }

    String getPlatform() {
        return platform
    }

    void setPlatform(String platform) {
        this.platform = platform
    }

    String getSaveName() {
        return saveName
    }

    void setSaveName(String saveName) {
        this.saveName = saveName
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

    String getDockerCmd() {
        return dockerCmd
    }

    void setDockerCmd(String dockerCmd) {
        this.dockerCmd = dockerCmd
    }
}
