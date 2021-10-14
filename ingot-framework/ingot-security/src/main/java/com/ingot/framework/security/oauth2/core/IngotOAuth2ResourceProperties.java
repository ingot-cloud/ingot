package com.ingot.framework.security.oauth2.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : IngotOAuth2ResourceProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 2:44 下午.</p>
 */
@ConfigurationProperties(prefix = "ingot.oauth2.resource")
public class IngotOAuth2ResourceProperties {

    @Getter
    @Setter
    private List<String> publicUrls = new ArrayList<>();
    @Getter
    @Setter
    private List<String> innerUrls = new ArrayList<>();

    public void addPublic(String url) {
        publicUrls.add(url);
    }

    public void addInner(String url) {
        innerUrls.add(url);
    }
}
