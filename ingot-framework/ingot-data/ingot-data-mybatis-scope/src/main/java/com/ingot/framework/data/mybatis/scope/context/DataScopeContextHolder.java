package com.ingot.framework.data.mybatis.scope.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;

/**
 * <p>数据范围上下文栈。嵌套 {@code @DataScope} 必须 push/pop；使用普通 {@link ThreadLocal}，异步不得继承。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DataScopeContextHolder {

    private static final ThreadLocal<Deque<Frame>> STACK = new ThreadLocal<>();

    private DataScopeContextHolder() {
    }

    /**
     * 压入一层数据范围。
     *
     * @param frame 当前资源操作的范围帧
     */
    public static void push(Frame frame) {
        Deque<Frame> stack = STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            STACK.set(stack);
        }
        stack.push(frame);
    }

    /**
     * 弹出最内层；栈空时清除 ThreadLocal。
     */
    public static void pop() {
        Deque<Frame> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            STACK.remove();
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    /**
     * 读取当前帧。
     *
     * @return 当前帧；无上下文时返回 {@code null}
     */
    public static Frame current() {
        Deque<Frame> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    /**
     * 当前帧是否跳过行过滤（ALL）。
     *
     * @return 跳过时返回 {@code true}
     */
    public static boolean isSkip() {
        Frame frame = current();
        return frame != null && BooleanUtil.isTrue(frame.skip());
    }

    /**
     * 当前帧部门范围。
     *
     * @return 部门 ID；无上下文返回空列表
     */
    public static List<Long> getScopes() {
        Frame frame = current();
        return frame == null ? List.of() : frame.deptIds();
    }

    /**
     * 当前帧本人范围对应用户 ID。
     *
     * @return 用户 ID；无 SELF 时为 {@code null}
     */
    public static Long getUserScope() {
        Frame frame = current();
        return frame == null ? null : frame.userId();
    }

    /**
     * 当前帧绑定的资源编码。
     *
     * @return 资源编码；无上下文为 {@code null}
     */
    public static String getResource() {
        Frame frame = current();
        return frame == null ? null : frame.resource();
    }

    /**
     * 当前是否存在可执行范围（含 skip / SELF / 部门）。
     *
     * @return 存在时返回 {@code true}
     */
    public static boolean isNotEmpty() {
        Frame frame = current();
        if (frame == null) {
            return false;
        }
        return BooleanUtil.isTrue(frame.skip())
                || frame.userId() != null
                || CollUtil.isNotEmpty(frame.deptIds());
    }

    /**
     * 当前无任何范围条件。
     *
     * @return 空范围时返回 {@code true}
     */
    public static boolean isEmpty() {
        return !isNotEmpty();
    }

    /**
     * 清除全部栈帧。仅用于测试或非 AOP 异常路径。
     */
    public static void clear() {
        STACK.remove();
    }

    /**
     * 一层 {@code (resource, permission)} 的执行上下文。
     *
     * @param resource 资源编码
     * @param permission 具体权限码
     * @param skip     ALL 时跳过行过滤
     * @param deptIds  部门并集
     * @param userId   SELF 对应用户；无 SELF 为 {@code null}
     */
    public record Frame(String resource, String permission, boolean skip, List<Long> deptIds, Long userId) {

        /**
         * 规范化空列表。
         *
         * @param resource   资源编码
         * @param permission 权限码
         * @param skip       是否跳过
         * @param deptIds    部门
         * @param userId     本人
         * @return 帧
         */
        public static Frame of(String resource, String permission, boolean skip, List<Long> deptIds, Long userId) {
            List<Long> copy = deptIds == null ? List.of() : List.copyOf(deptIds);
            return new Frame(StrUtil.blankToDefault(resource, null), permission, skip, copy, userId);
        }
    }
}
