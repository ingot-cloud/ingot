package com.ingot.cloud.iam.domain.workbook;

/**
 * <p>集中部门表格逐行与整份失败说明，供错误报告与任务失败码复用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DepartmentWorkbookReasons {
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
    public static final String ROW_LIMIT_EXCEEDED = "数据行不能超过 " + DepartmentWorkbookLimits.MAX_DATA_ROWS + " 行";
    /**
     * 部门名称为空。
     */
    public static final String NAME_REQUIRED = "请填写部门名称";
    /**
     * 部门名称含路径分隔符或超长。
     */
    public static final String NAME_INVALID = "部门名称不能包含 /，且不超过 "
            + DepartmentWorkbookLimits.NAME_MAX_LENGTH + " 个字符";
    /**
     * 上级部门为空。
     */
    public static final String PARENT_REQUIRED = "请填写上级部门";
    /**
     * 创建行填写了部门 ID。
     */
    public static final String ID_ON_CREATE = "新建部门不能填写部门 ID";
    /**
     * 更新行缺少部门 ID。
     */
    public static final String DEPARTMENT_ID_REQUIRED = "请填写部门 ID";
    /**
     * 部门不存在或不在范围。
     */
    public static final String DEPARTMENT_NOT_FOUND = "部门不存在或不可访问";
    /**
     * 上级路径不存在。
     */
    public static final String PARENT_NOT_FOUND = "上级部门路径不存在";
    /**
     * 上级路径对应多个部门。
     */
    public static final String PARENT_AMBIGUOUS = "上级部门路径不唯一";
    /**
     * 同级名称重复。
     */
    public static final String NAME_DUPLICATE = "同一上级下部门名称必须唯一";
    /**
     * 根部门不能改上级。
     */
    public static final String ROOT_PARENT = "根部门不能变更上级";
    /**
     * 不能通过导入再造根部门。
     */
    public static final String ROOT_CREATE = "不能通过导入创建根部门";
    /**
     * 新上级是自身或下级，会形成环。
     */
    public static final String PARENT_CYCLE = "上级部门不能形成环";
    /**
     * 排序不是整数。
     */
    public static final String SORT_INVALID = "排序必须是整数";
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
    public static final String EXPORT_DENIED = "没有部门导出权限，请确认权限后重新导出";
    /**
     * 导出生成失败，展示给发起人。
     */
    public static final String EXPORT_FAILED = "部门导出失败，请重新导出";

    private DepartmentWorkbookReasons() {
    }
}
