package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>标识精确 ACTION 末段声明的操作语义，并给出该语义是否改变持久状态。</p>
 *
 * <p>授权热路径据此决定能否使用缓存快照：只读操作允许命中未过期的授权视图，
 * 改变状态的操作必须按最新事实重新求值，避免用旧快照放行写入。字面量与
 * {@link IamAction#getCode()} 的第三段一致，新增 ACTION 时若出现新词汇必须先在此登记。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see IamAction#getOperation()
 */
@Getter
@RequiredArgsConstructor
public enum IamActionOperation {
    /**
     * 读取列表、详情或树。
     */
    READ("read", false),
    /**
     * 试算结果，不落库。
     */
    PREVIEW("preview", false),
    /**
     * 诊断授权来源，不执行目标操作。
     */
    DIAGNOSE("diagnose", false),
    /**
     * 导出，按导出时身份重验后读取。
     */
    EXPORT("export", false),
    /**
     * 新建对象。
     */
    CREATE("create", true),
    /**
     * 修改既有对象。
     */
    UPDATE("update", true),
    /**
     * 删除对象。
     */
    DELETE("delete", true),
    /**
     * 暂停或恢复。
     */
    STATUS("status", true),
    /**
     * 发布版本或策略。
     */
    PUBLISH("publish", true),
    /**
     * 移出成员资格，不删除账号。
     */
    REMOVE("remove", true),
    /**
     * 把已分配主体升级到新角色版本。
     */
    UPGRADE("upgrade", true),
    /**
     * 转交组织所有者。
     */
    OWNER_TRANSFER("owner-transfer", true),
    /**
     * 整体替换成员任职部门。
     */
    DEPARTMENTS("departments", true),
    /**
     * 受限精确查找，不返回组织关系。
     */
    LOOKUP("lookup", false),
    /**
     * 启用全局账号。
     */
    ENABLE("enable", true),
    /**
     * 停用全局账号。
     */
    DISABLE("disable", true),
    /**
     * 手动锁定账号。
     */
    LOCK("lock", true),
    /**
     * 手动解锁账号。
     */
    UNLOCK("unlock", true),
    /**
     * 管理员重置密码。
     */
    RESET_PASSWORD("reset-password", true),
    /**
     * 撤销会话或令牌。
     */
    REVOKE("revoke", true);

    /**
     * ACTION 码末段使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;
    /**
     * 该操作是否改变持久状态。
     */
    private final boolean mutating;

    private static final Map<String, IamActionOperation> BY_VALUE =
            EnumUtils.index(values(), IamActionOperation::getValue);
    private static final String CODE_SEPARATOR = ":";

    /**
     * 按 ACTION 码末段解析操作语义。
     *
     * @param value 操作末段字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未登记
     */
    @JsonCreator
    public static IamActionOperation getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

    /**
     * 从完整 ACTION 码解析操作语义。
     *
     * @param code 形如 {@code iam-tenant:member:status} 的精确操作码
     * @return 对应枚举
     * @throws IllegalArgumentException 码不含操作段，或末段未登记
     */
    public static IamActionOperation ofActionCode(String code) {
        if (code == null) {
            return null;
        }
        int separator = code.lastIndexOf(CODE_SEPARATOR);
        if (separator < 0 || separator == code.length() - 1) {
            throw new IllegalArgumentException("ACTION 码缺少操作段: " + code);
        }
        return getEnum(code.substring(separator + 1));
    }
}
