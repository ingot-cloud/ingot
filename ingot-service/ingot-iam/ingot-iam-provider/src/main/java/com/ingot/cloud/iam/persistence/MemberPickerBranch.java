package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;

/**
 * <p>仅供组织成员选择器使用的可信分支条件，不改变通用列表及真实任职。</p>
 * @author jy
 * @since 1.0.0
 * @param departmentId 服务端验证可见的浏览部门；空值表示未指定分支
 * @param root 是否为已验证的根部门
 * @param empty 是否禁止初始片段入口列出成员
 */
public record MemberPickerBranch(BigInteger departmentId, boolean root, boolean empty) {
    /** 保留业务场景的整个候选池，供搜索或无部门读取资格使用。 */
    public static MemberPickerBranch all() { return new MemberPickerBranch(null, false, false); }
    /** 根不可见时，先只显示可浏览部门片段。 */
    public static MemberPickerBranch none() { return new MemberPickerBranch(null, false, true); }
    /** 根据已验证部门构造分支。 */
    public static MemberPickerBranch department(long id, boolean root) { return new MemberPickerBranch(BigInteger.valueOf(id), root, false); }
    /** 使用内部固定列及绑定参数路径；部门关系判断先于分页与计数。 */
    public String sql(String member, String tenant, String departmentBinding) {
        if (empty) return "1=0";
        if (departmentId == null) return "1=1";
        String relation = "SELECT 1 FROM iam_member_department picker_md WHERE picker_md.tenant_id=" + tenant
                + " AND picker_md.member_id=" + member;
        String direct = "EXISTS(" + relation + " AND picker_md.department_id=" + departmentBinding + ")";
        return root ? "(" + direct + " OR NOT EXISTS(" + relation + "))" : direct;
    }
}
