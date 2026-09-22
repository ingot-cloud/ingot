package com.ingot.cloud.iam.support;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>解析并校验 IAM 正数标识，拒绝客户端伪造的非数字或非正数 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamIds {
    private IamIds() {
    }

    /**
     * 将路径或载荷中的 ID 解析为正数。
     *
     * @param value 字符串标识
     * @return 正数 ID
     * @throws BizException 缺失、非数字或非正数时使用 InvalidArgument
     */
    public static long require(String value) {
        return require(value, IamReasonCode.INVALID_ARGUMENT);
    }

    /**
     * 将标识解析为正数，使用调用方指定的失败码。
     *
     * @param value 字符串标识
     * @param failure 解析失败时的业务码
     * @return 正数 ID
     */
    public static long require(String value, IamReasonCode failure) {
        if (value == null || value.isBlank()) {
            throw new BizException(failure);
        }
        try {
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw new BizException(failure);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BizException(failure);
        }
    }

    /**
     * 解析可选 ID，空白表示不限制。
     *
     * @param value 字符串标识
     * @return 正数 ID；空白为 {@code null}
     */
    public static Long optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return require(value);
    }

    /**
     * 解析逗号分隔的 ID 列表，空白表示不限制。
     *
     * @param value 逗号分隔标识
     * @return 正数 ID；空白为空列表
     */
    public static List<Long> optionalList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                ids.add(require(part.trim()));
            }
        }
        return ids;
    }

    /**
     * 将数据库标识格式化为契约使用的十进制字符串。
     *
     * @param id 正数 ID
     * @return 十进制文本
     */
    public static String text(long id) {
        return Long.toString(id);
    }

    /**
     * 校验乐观锁版本字面量形状，再与当前值比较。
     *
     * @param expected 客户端提交的预期版本
     * @param actual 数据库当前版本
     */
    public static void requireVersion(String expected, String actual) {
        if (expected == null || !expected.matches("0|[1-9][0-9]{0,19}")) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        requireExpected(expected, actual);
    }

    /**
     * 比较任意形状的预期版本，不允许空值。
     *
     * @param expected 客户端提交的预期版本
     * @param actual 当前配置版本
     */
    public static void requireExpected(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (!expected.equals(actual)) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
    }
}
