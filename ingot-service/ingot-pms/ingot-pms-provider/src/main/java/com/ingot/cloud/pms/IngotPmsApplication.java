package com.ingot.cloud.pms;

import com.ingot.framework.security.annotation.EnableIngotResourceServer;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>Description  : IngotPmsApplication.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:15 下午.</p>
 */
@EnableIngotResourceServer
@SpringCloudApplication
@EnableTransactionManagement
public class IngotPmsApplication {

    public static void main(String[] args){
        SpringApplication.run(IngotPmsApplication.class, args);
    }
}
