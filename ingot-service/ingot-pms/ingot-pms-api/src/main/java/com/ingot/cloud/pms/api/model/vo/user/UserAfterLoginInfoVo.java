package com.ingot.cloud.pms.api.model.vo.user;

import com.ingot.cloud.pms.api.model.vo.menu.MenuVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : UserAfterLoginInfoDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 2:29 PM.</p>
 */
@Data
public class UserAfterLoginInfoVo implements Serializable {
    private String username;
    private String group_id;
    private String group_name;
    private List<String> role_list;
    private List<MenuVo> menu_list;
}
