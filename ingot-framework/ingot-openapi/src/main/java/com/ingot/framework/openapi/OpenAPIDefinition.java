package com.ingot.framework.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : OpenAPIDefinition.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/20.</p>
 * <p>Time         : 09:47.</p>
 */
@Slf4j
public class OpenAPIDefinition extends OpenAPI implements InitializingBean, ApplicationContextAware {

    @Setter
    private String path;

    private ApplicationContext applicationContext;

    private SecurityScheme securityScheme(OpenAPIProperties openAPIProperties) {
        OAuthFlow clientCredential = new OAuthFlow();
        clientCredential.setTokenUrl(openAPIProperties.getTokenUrl());
        clientCredential.setScopes(new Scopes().addString(openAPIProperties.getScope(), openAPIProperties.getScope()));
        OAuthFlows oauthFlows = new OAuthFlows();
        oauthFlows.password(clientCredential);
        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.OAUTH2);
        securityScheme.setFlows(oauthFlows);
        return securityScheme;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OpenAPIProperties openAPIProperties = applicationContext.getBean(OpenAPIProperties.class);
        this.info(new Info().title(openAPIProperties.getTitle()));
        // oauth2.0 password
        this.schemaRequirement(HttpHeaders.AUTHORIZATION, this.securityScheme(openAPIProperties));
        // servers
        List<Server> serverList = new ArrayList<>();
        serverList.add(new Server().url(openAPIProperties.getGateway() + "/" + path));
        this.servers(serverList);
        // 支持参数平铺
        SpringDocUtils.getConfig().addSimpleTypesForParameterObject(Class.class);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
