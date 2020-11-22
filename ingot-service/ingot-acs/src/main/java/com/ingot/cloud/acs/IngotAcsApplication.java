package com.ingot.cloud.acs;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

/**
 * <p>Description  : IngotAcsApplication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/27.</p>
 * <p>Time         : 11:37 上午.</p>
 */
@EnableHystrix
@SpringCloudApplication
public class IngotAcsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngotAcsApplication.class, args);
    }
}
