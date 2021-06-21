package com.ingot.cloud.pms.api.model.vo.tenant;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : SimpleTenantVo.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/21.</p>
 * <p>Time         : 5:51 下午.</p>
 */
@Data
public class SimpleTenantVo implements Serializable {
    private int id;
    private String name;

    public SimpleTenantVo(SysTenant tenant){
        this.id = tenant.getId();
        this.name = tenant.getName();
    }
}
