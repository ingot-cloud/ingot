package com.ingot.cloud.iam.api.rpc;

import com.ingot.cloud.iam.api.model.dto.auth.LoginRecordDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>声明 IAM 登录记录写入 RPC。</p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@FeignClient(contextId = "iamLoginRecordService", value = ServiceNameConstants.IAM_SERVICE)
public interface RemoteIamLoginRecordService {

    /**
     * 记录用户登录事件（成功或失败）
     *
     * @param dto 登录记录 DTO
     * @return 处理结果
     */
    @PostMapping("/inner/user/login/record")
    R<Void> record(@RequestBody LoginRecordDTO dto);
}
