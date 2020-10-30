package com.ingot.framework.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * <p>Description  : AutoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/7.</p>
 * <p>Time         : 下午1:44.</p>
 */
@Slf4j
@Configuration
@ComponentScan(value = {
        "com.ingot.framework.security"
})
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
public class SecurityAutoConfiguration {

    @Bean
    @Primary
    @LoadBalanced
    public RestTemplate lbRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public void handleError(@NonNull ClientHttpResponse response) throws IOException {
                log.info(">>> restTemplate handleError={}", response.getRawStatusCode());
                if (response.getRawStatusCode() != HttpStatus.BAD_REQUEST.value()) {
                    super.handleError(response);
                }
            }
        });
        return restTemplate;
    }
}
