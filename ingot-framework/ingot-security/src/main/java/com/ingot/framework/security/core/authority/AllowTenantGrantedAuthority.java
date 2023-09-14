package com.ingot.framework.security.core.authority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.security.jackson2.IngotSecurityJackson2Modules;
import org.springframework.util.Assert;

/**
 * <p>Description  : AllowTenantGrantedAuthority.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/14.</p>
 * <p>Time         : 10:09 AM.</p>
 */
public class AllowTenantGrantedAuthority implements IngotGrantedAuthority<AllowTenantDTO> {
    private final AllowTenantDTO allow;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AllowTenantGrantedAuthority(AllowTenantDTO allow) {
        Assert.notNull(allow, "A granted authority object representation is required");
        this.allow = allow;
        IngotSecurityJackson2Modules.registerModules(objectMapper, AllowTenantGrantedAuthority.class.getClassLoader());
    }

    @Override
    public AllowTenantDTO extract() {
        return this.allow;
    }

    @Override
    public String getAuthority() {
        try {
            return objectMapper.writeValueAsString(this.allow);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AllowTenantGrantedAuthority allowGrantedAuthority) {
            return this.allow.getId() == allowGrantedAuthority.allow.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.allow.getId());
    }

    @Override
    public String toString() {
        return this.getAuthority();
    }
}
