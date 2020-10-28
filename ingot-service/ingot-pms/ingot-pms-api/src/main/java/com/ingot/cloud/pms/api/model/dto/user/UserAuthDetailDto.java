package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.cloud.pms.api.model.vo.user.UserVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : UserAuthDetailDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 2:07 PM.</p>
 */
@Data
public class UserAuthDetailDto implements Serializable {

    private UserVo user;
    private String authType;
    private List<String> roleList;
}
