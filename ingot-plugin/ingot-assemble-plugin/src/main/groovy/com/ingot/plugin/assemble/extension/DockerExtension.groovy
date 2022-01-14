package com.ingot.plugin.assemble.extension

import lombok.Data

/**
 * <p>Description  : DockerExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/11.</p>
 * <p>Time         : 11:25 上午.</p>
 */
@Data
class DockerExtension {

    /**
     * docker命令path
     */
    String dockerCmd

    /**
     * dockerfile 存储目录
     */
    String dockerfileDir

    /**
     * 注册中心地址
     */
    String registry

    /**
     * 注册中心登录用户名
     */
    String username

    /**
     * 注册中心登录密码
     */
    String password

    /**
     * 存储库名称
     */
    String name


    DockerExtension() {}
}
