package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * <p>删除账号用例（入站端口）</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface DeleteAccountUseCase {

    /**
     * 删除账号
     * @param command 删除账号命令
     */
    void deleteAccount(DeleteAccountCommand command);

    @Value
    @Builder
    class DeleteAccountCommand {

        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 操作人ID
         */
        Long operatorId;

        /**
         * 操作人姓名
         */
        String operatorName;

        /**
         * 操作来源（IAM 管理端 / MEMBER 会员端 等）
         */
        EventSource source;
    }
}
