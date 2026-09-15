package com.ingot.cloud.iam.support;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;

import java.util.List;

/**
 * <p>规范化管理面分页参数，避免客户端用非法页码绕过范围过滤。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamPages {
    /**
     * 契约默认页码，从 1 开始。
     */
    public static final int DEFAULT_PAGE = 1;
    /**
     * 契约默认页大小。
     */
    public static final int DEFAULT_SIZE = 20;
    /**
     * 单页上限，防止一次拉出未过滤全集。
     */
    public static final int MAX_SIZE = 200;

    private IamPages() {
    }

    /**
     * 校验页码与页大小。
     *
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     */
    public static void require(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_SIZE) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    /**
     * 计算 SQL OFFSET。
     *
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     * @return 非负偏移
     */
    public static int offset(int page, int pageSize) {
        require(page, pageSize);
        return (page - 1) * pageSize;
    }

    /**
     * 组装详情分页响应。
     *
     * @param items 当前页
     * @param total 相同过滤条件下的总数
     * @param page 页码
     * @param pageSize 页大小
     * @param <T> 记录类型
     * @return 分页信封
     */
    public static <T> PageResponse<ResourceDetail<T>> details(List<ResourceDetail<T>> items, long total,
                                                             int page, int pageSize) {
        require(page, pageSize);
        return new PageResponse<>(items, total, page, pageSize);
    }
}
