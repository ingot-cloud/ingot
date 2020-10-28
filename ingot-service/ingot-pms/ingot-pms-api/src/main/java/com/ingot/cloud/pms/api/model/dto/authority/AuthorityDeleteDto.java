package com.ingot.cloud.pms.api.model.dto.authority;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>Description  : AuthorityDeleteDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/7.</p>
 * <p>Time         : 9:24 AM.</p>
 */
@Data
public class AuthorityDeleteDto implements Serializable {
    @NotBlank(message = "id不能为空")
    private String id;
}
