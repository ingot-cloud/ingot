package com.ingot.cloud.bff.model.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import lombok.Data;
import lombok.ToString;

/**
 * <p>
 * BffLoginDTO 只接受本登录阶段明确允许的业务字段。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class BffLoginDTO {

    /** 当前绑定新建的事务。 */
    private String transactionId;

    /** 账号。 */
    private String username;

    /** 只在本次请求中使用的密码。 */
    private String password;

    /** 拒绝未声明字段，不能静默忽略客户端 OAuth 或重定向参数。 */
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
    }

}
