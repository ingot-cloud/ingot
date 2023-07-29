package com.ingot.framework.security.oauth2.server.authorization.code;

import com.ingot.framework.security.core.userdetails.IngotUser;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : PreAuthorization.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/29.</p>
 * <p>Time         : 11:30 AM.</p>
 */
@Data
public class PreAuthorization implements Serializable {
    private String clientId;
    private String username;

    public static PreAuthorization create(IngotUser user) {
        PreAuthorization result = new PreAuthorization();
        result.setClientId(user.getClientId());
        result.setUsername(user.getUsername());
        return result;
    }
}
