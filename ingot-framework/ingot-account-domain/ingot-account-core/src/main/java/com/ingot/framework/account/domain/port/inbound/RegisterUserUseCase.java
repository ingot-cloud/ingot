package com.ingot.framework.account.domain.port.inbound;

import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.model.enums.EventSource;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 注册用户用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface RegisterUserUseCase {

    /**
     * 注册/创建用户
     * <p>
     * 同一方法覆盖两种创建来源，行为由 {@link RegisterUserCommand#getCreationSource()} 控制：
     * <ul>
     *   <li>{@link CreationSource#SELF_REGISTER}：用户自主注册，执行密码强度校验</li>
     *   <li>{@link CreationSource#ADMIN_CREATE}：管理员/系统创建，跳过强度校验，
     *       并默认将 {@code mustChangePwd} 置为 {@code true}（命令中明确传入 {@code false} 除外）</li>
     * </ul>
     *
     * @param command 创建命令
     * @return 创建的用户账号
     */
    UserAccount register(RegisterUserCommand command);

    /**
     * 账号创建来源
     */
    enum CreationSource {
        /**
         * 用户自主注册：执行密码强度等凭证策略校验
         */
        SELF_REGISTER,

        /**
         * 管理员/系统创建：跳过凭证策略校验，密码通常为随机值，用户首次登录须修改
         */
        ADMIN_CREATE
    }

    /**
     * 注册用户命令
     */
    @Value
    @Builder
    class RegisterUserCommand {
        /**
         * 账号创建来源，控制是否执行凭证策略校验
         * <p>为 {@code null} 时等同于 {@link CreationSource#SELF_REGISTER}</p>
         */
        CreationSource creationSource;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 用户名
         */
        String username;

        /**
         * 原始密码（UseCase 内部会加密）
         * <p>{@link CreationSource#ADMIN_CREATE} 场景下通常传入随机生成的密码</p>
         */
        String password;

        /**
         * 手机号
         */
        String phone;

        /**
         * 邮箱
         */
        String email;

        /**
         * 昵称
         */
        String nickname;

        /**
         * 头像
         */
        String avatar;

        /**
         * 创建人ID
         */
        Long createdBy;

        /**
         * 是否强制修改密码
         * <p>默认强制修改</p>
         */
        Boolean mustChangePwd;

        /**
         * 账号创建事件来源（可选）
         * <p>管理员创建场景通常传 {@link EventSource#PMS} / {@link EventSource#MEMBER}
         * 等具体业务侧来源；为 {@code null} 时用例将按 {@code userType} 自动派生
         * （{@code ADMIN} → {@link EventSource#PMS}、{@code APP} → {@link EventSource#MEMBER}），
         * 自助注册场景一律落为 {@link EventSource#SYSTEM}。</p>
         */
        EventSource eventSource;
    }
}
