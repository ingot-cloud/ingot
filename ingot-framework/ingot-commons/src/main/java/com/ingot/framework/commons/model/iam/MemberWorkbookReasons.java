package com.ingot.framework.commons.model.iam;

/**
 * <p>集中成员表格逐行与整份失败说明，供错误报告与任务失败码复用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MemberWorkbookReasons {
    /**
     * 文件不是可解析的 xlsx。
     */
    public static final String FILE_INVALID = "请上传 XLSX 文件";
    /**
     * 找不到表头。
     */
    public static final String HEADER_MISSING = "未找到有效表头";
    /**
     * 超过数据行上限。
     */
    public static final String ROW_LIMIT_EXCEEDED = "数据行不能超过 " + MemberWorkbookLimits.MAX_DATA_ROWS + " 行";
    /**
     * 姓名为空。
     */
    public static final String NAME_REQUIRED = TenantMemberCreateInput.DISPLAY_NAME_REQUIRED;
    /**
     * 手机号为空。
     */
    public static final String PHONE_REQUIRED = TenantMemberCreateInput.PHONE_REQUIRED;
    /**
     * 手机号形态不合法。
     */
    public static final String PHONE_INVALID = TenantMemberCreateInput.PHONE_INVALID;
    /**
     * 更新行缺少成员 ID。
     */
    public static final String MEMBER_ID_REQUIRED = "请填写成员 ID";
    /**
     * 成员不存在或不在范围。
     */
    public static final String MEMBER_NOT_FOUND = "成员不存在或不可访问";
    /**
     * 已移出成员拒绝回写。
     */
    public static final String MEMBER_REMOVED = "已移出成员不能更新";
    /**
     * 单元格含脱敏星号。
     */
    public static final String MASKED_VALUE = "脱敏值不能写回";
    /**
     * 部门路径不存在。
     */
    public static final String DEPARTMENT_NOT_FOUND = "部门路径不存在";
    /**
     * 部门路径对应多个部门。
     */
    public static final String DEPARTMENT_AMBIGUOUS = "部门路径不唯一";
    /**
     * 组织拥有者不能改任职。
     */
    public static final String OWNER_DEPARTMENTS = "组织拥有者不能变更部门";
    /**
     * 快照或导入执行失败粗码。
     */
    public static final String FAILURE_PROCESS = "PROCESS_FAILED";
    /**
     * 身份失效粗码。
     */
    public static final String FAILURE_IDENTITY = "IDENTITY_INVALID";
    /**
     * 导出时已失去导出权限或身份失效，展示给发起人。
     */
    public static final String EXPORT_DENIED = "没有成员导出权限，请确认权限后重新导出";
    /**
     * 导出生成失败，展示给发起人。
     */
    public static final String EXPORT_FAILED = "成员导出失败，请重新导出";

    private MemberWorkbookReasons() {
    }
}
