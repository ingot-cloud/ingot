package com.ingot.cloud.auth.api.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>在线会话分页结果，字段与全局 {@code IPage} 序列化形态一致。</p>
 *
 * <p>Inner 契约不复用 MyBatis-Plus 的 {@code IPage}，避免 {@code ingot-auth-api} 为一个出参
 * 结构引入持久层依赖；字段名保持 {@code current / size / total / records}，前端与
 * {@code IPage} 分页保持同一套解析逻辑。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerSessionPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码，从 1 开始。
     */
    private long current;

    /**
     * 每页条数。
     */
    private long size;

    /**
     * 满足条件的总记录数。
     */
    private long total;

    private List<InnerSessionVO> records;

    /**
     * 构造空页，保留请求的分页参数。
     */
    public static InnerSessionPageVO empty(long current, long size) {
        return InnerSessionPageVO.builder()
                .current(current)
                .size(size)
                .total(0L)
                .records(new ArrayList<>())
                .build();
    }
}
