package com.ingot.plugin.assemble.extension

/**
 * <p>Description  : DockerExtension.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/1/11.</p>
 * <p>Time         : 11:25 上午.</p>
 */
class DockerExtension {

    /**
     * 注册中心地址
     */
    String registry;

    /**
     * 注册中心登录用户名
     */
    String username;

    /**
     * 注册中心登录密码
     */
    String password;

    /**
     * 存储库
     */
    String repository;


    DockerExtension() {}
}
