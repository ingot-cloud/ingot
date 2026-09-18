package com.ingot.cloud.bff.model.dto;

import lombok.Data;

/**
 * 管理台交接：仅一次性 ticket。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class BffCompleteDTO {
    private String ticket;
}
