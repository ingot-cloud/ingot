package com.ingot.cloud.pms.api.model.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ingot.framework.crypto.annotation.IngotFieldDecrypt;
import com.ingot.framework.crypto.jackson.CryptoDeserializer;
import com.ingot.framework.crypto.model.CryptoType;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserPasswordDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/23.</p>
 * <p>Time         : 9:59 下午.</p>
 */
@Data
public class UserPasswordDTO implements Serializable {
    @IngotFieldDecrypt(CryptoType.AES)
    @JsonDeserialize(using = CryptoDeserializer.class)
    private String password;
    @IngotFieldDecrypt(CryptoType.AES)
    @JsonDeserialize(using = CryptoDeserializer.class)
    private String newPassword;
}
