package com.ingot.cloud.iam.application.account;

import java.util.List;
import java.util.Objects;

import com.ingot.cloud.iam.api.model.dto.user.InnerUserDTO;
import com.ingot.cloud.iam.infrastructure.persistence.AccountCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>为内部账号RPC读取当前账号与框架锁定事实，不返回凭证或替代成员身份。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AccountDetailsQueryService {
    private final AccountCredentialRepository accounts;

    /** 按账号ID读取内部资料；账号不存在时保持空响应。 */
    public InnerUserDTO find(Long id) {
        return accounts.findById(id).map(account -> accounts.toInnerUser(account, accounts.locked(id))).orElse(null);
    }

    /** 保留请求顺序，仅返回实际存在的账号。 */
    public List<InnerUserDTO> list(List<Long> ids) {
        return ids.stream().map(this::find).filter(Objects::nonNull).toList();
    }
}
