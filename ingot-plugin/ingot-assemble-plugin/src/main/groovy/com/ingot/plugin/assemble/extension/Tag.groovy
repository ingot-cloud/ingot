package com.ingot.plugin.assemble.extension

/**
 * <p>Description  : Test.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/18.</p>
 * <p>Time         : 11:26 上午.</p>
 */
class Tag {
    /**
     * 存储库名称
     */
    private String name

    /**
     * dockerfile 存储目录
     */
    private String dockerfileDir

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
}
