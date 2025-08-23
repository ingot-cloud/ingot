package com.ingot.cloud.pms.api.model.dto.user;

import java.io.Serializable;

import com.ingot.framework.crypto.annotation.InFieldDecrypt;
import com.ingot.framework.crypto.model.CryptoType;
import lombok.Data;

/**
 * <p>Description  : UserPasswordDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/23.</p>
 * <p>Time         : 9:59 下午.</p>
 */
@Data
public class UserPasswordDTO implements Serializable {
    @InFieldDecrypt(CryptoType.AES)
    private String password;
    @InFieldDecrypt(CryptoType.AES)
    private String newPassword;
}
