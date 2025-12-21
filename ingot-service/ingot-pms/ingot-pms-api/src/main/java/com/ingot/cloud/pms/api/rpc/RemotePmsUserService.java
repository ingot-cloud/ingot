package com.ingot.cloud.pms.api.rpc;

import java.util.List;

import com.ingot.cloud.pms.api.model.dto.user.InnerUserDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Description  : PmsUserService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/4.</p>
 * <p>Time         : 09:17.</p>
 */
@FeignClient(contextId = "pmsUserService", value = ServiceNameConstants.PMS_SERVICE)
public interface RemotePmsUserService {

    @GetMapping("/inner/user/{id}")
    R<InnerUserDTO> getUserInfo(@PathVariable Long id);

    @GetMapping("/inner/user/list")
    R<List<InnerUserDTO>> getAllUserInfo(@RequestBody List<Long> ids);
}
