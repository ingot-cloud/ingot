package com.ingot.framework.commons.model.download;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>下载中心任务的生命周期，所有接入的导出共用同一组状态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum DownloadTaskStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    EXPIRED("EXPIRED");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, DownloadTaskStatus> BY_VALUE =
            EnumUtils.index(values(), DownloadTaskStatus::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static DownloadTaskStatus getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

    /**
     * 是否仍在排队或生成。
     *
     * @return PENDING 或 RUNNING 时为 true
     */
    public boolean active() {
        return this == PENDING || this == RUNNING;
    }
}
