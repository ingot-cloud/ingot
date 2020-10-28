package com.ingot.cloud.pms.api.model.dto.client;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AppQueryParams.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/10.</p>
 * <p>Time         : 11:30 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClientQueryParams extends BaseQueryDto {

    private String client_id;
    private String client_name;
    private String type;

    @Override public String uniqueKey() {
        return super.uniqueKey() + "clientId" + client_id + "clientName" + client_name + "type" + type;
    }
}
