package com.ingot.cloud.pms.api.model.dto.client;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : ClientBindMenuDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/3.</p>
 * <p>Time         : 11:34 AM.</p>
 */
@Data
public class ClientBindMenuDto implements Serializable {
    private String client_id;
    private List<Long> menu_ids;
    private List<Long> delete_menu_ids;
}
