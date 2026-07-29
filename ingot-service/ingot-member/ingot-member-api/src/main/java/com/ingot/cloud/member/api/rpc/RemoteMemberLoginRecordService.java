package com.ingot.cloud.member.api.rpc;

import com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Member 登录记录 Feign 接口。
 *
 * <p>Auth 服务通过此接口通知 Member 异步处理 C 端登录成功/失败事件：
 * 更新 last_login_at、last_login_ip、failed_login_count，以及触发自动锁定逻辑。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "memberLoginRecordService", value = ServiceNameConstants.MEMBER_SERVICE)
public interface RemoteMemberLoginRecordService {

    /**
     * 记录用户登录事件（成功或失败）。
     *
     * @param dto 登录记录 DTO
     * @return 处理结果
     */
    @PostMapping("/inner/user/login/record")
    R<Void> record(@RequestBody LoginRecordDTO dto);
}
