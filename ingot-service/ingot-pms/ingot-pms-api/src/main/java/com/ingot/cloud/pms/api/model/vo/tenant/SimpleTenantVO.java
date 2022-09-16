package com.ingot.cloud.pms.api.model.vo.tenant;

import java.io.Serializable;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import lombok.Data;

/**
 * <p>Description  : SimpleTenantVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/21.</p>
 * <p>Time         : 5:51 下午.</p>
 */
@Data
public class SimpleTenantVO implements Serializable {
    private int id;
    private String name;

    public SimpleTenantVO(SysTenant tenant){
        this.id = tenant.getId();
        this.name = tenant.getName();
    }
}
