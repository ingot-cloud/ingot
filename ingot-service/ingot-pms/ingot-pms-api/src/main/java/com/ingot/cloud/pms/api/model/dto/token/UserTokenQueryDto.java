package com.ingot.cloud.pms.api.model.dto.token;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserTokenQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/12.</p>
 * <p>Time         : 2:13 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserTokenQueryDto extends BaseQueryDto {
    private String user_id;
    private String username;
    private String real_name;
    private int status = -1;
}
