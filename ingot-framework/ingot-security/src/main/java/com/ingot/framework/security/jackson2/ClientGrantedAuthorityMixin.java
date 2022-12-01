package com.ingot.framework.security.jackson2;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * <p>Description  : ClientGrantedAuthorityMixin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/1.</p>
 * <p>Time         : 4:52 PM.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ClientGrantedAuthorityMixin {
    /**
     * Mixin Constructor.
     * @param role the role
     */
    @JsonCreator
    public ClientGrantedAuthorityMixin(@JsonProperty("authority") String role) {
    }
}
