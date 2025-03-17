package com.ingot.framework.security.oauth2.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : InOAuth2ResourceProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 2:44 下午.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.oauth2.resource")
public class InOAuth2ResourceProperties {
    private List<String> publicUrls = new ArrayList<>();
    private List<String> innerUrls = new ArrayList<>();

    public void addPublic(String url) {
        publicUrls.add(url);
    }

    public void addInner(String url) {
        innerUrls.add(url);
    }
}
