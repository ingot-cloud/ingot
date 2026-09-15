package com.ingot.cloud.iam.persistence.projection;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>承载同一数据库快照中的有效成员与账号、成员、组织版本，不读取凭证。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
public class IdentityRow {
    /** 当前域成员 ID。 */
    private String memberId;
    /** 全局账号版本。 */
    private String accountVersion;
    /** 当前成员版本。 */
    private String memberVersion;
    /** 租户版本；平台域为空。 */
    private String tenantVersion;
}
