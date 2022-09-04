package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>Description  : DeptRoleScopeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/1.</p>
 * <p>Time         : 3:08 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DeptRoleScopeEnum {
    /**
     * 当前部门所有角色
     */
    CURRENT("0", "当前部门"),
    /**
     * 当前部门角色和其子部门角色；
     * 子部门角色深度获取，直到部门scope为CURRENT或者不含任何子部门时停止
     */
    CURRENT_CHILD("1", "当前部门以及子部门");

    /**
     * 状态
     */
    @JsonValue
    @EnumValue
    private final String value;

    /**
     * 描述
     */
    private final String desc;
}
