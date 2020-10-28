package com.ingot.cloud.pms.api.model.dto.client;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : AppGrantQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/11.</p>
 * <p>Time         : 11:10 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClientGrantQueryDto extends BaseQueryDto {
    private String client_id;
    private String client_name;
    // 资源id列表
    private List<String> list;

    @Override public String uniqueKey() {
        return super.uniqueKey() + "clientId" + client_id + "clientName" + client_name;
    }
}
