package com.ingot.cloud.iam.support;

import com.ingot.cloud.iam.identity.ActiveIdentity;

/**
 * <p>一次管理命令入口的准入结果：可信身份加上该 ACTION 是否来自完整治理资格。</p>
 *
 * <p>治理资格来自非委派授权。只凭委派派生授权的受限方在写入时必须绑定属于自己的单一有效委派，
 * 不能省略来源、借用他人委派或拼接多条委派的不同维度。</p>
 *
 * @param actor 状态有效的当前成员
 * @param governed 该 ACTION 至少来自一条非委派授权时为 true
 * @author jy
 * @since 1.0.0
 */
public record IamAdmission(ActiveIdentity actor, boolean governed) {
}
