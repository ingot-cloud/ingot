package com.ingot.framework.commons.model.iam;

/**
 * <p>集中成员表格列名、分隔符与 MIME，禁止调用点裸写表头字面量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MemberWorkbookColumns {
    /**
     * 更新导出与回写使用的成员 ID 列。
     */
    public static final String MEMBER_ID = "成员 ID";
    /**
     * 显示名列。
     */
    public static final String DISPLAY_NAME = "姓名";
    /**
     * 组织通讯录手机号列。
     */
    public static final String PHONE = "手机号";
    /**
     * 组织通讯录邮箱列。
     */
    public static final String EMAIL = "邮箱";
    /**
     * 任职路径列。
     */
    public static final String DEPARTMENTS = "部门";
    /**
     * 错误报告的 Excel 行号列。
     */
    public static final String ROW_NUMBER = "行号";
    /**
     * 错误报告的失败原因列。
     */
    public static final String REASON = "原因";
    /**
     * 一次性密码列。
     */
    public static final String INITIAL_PASSWORD = "初始密码";
    /**
     * 部门路径从根到自身的分层符。
     */
    public static final String PATH_SEPARATOR = "/";
    /**
     * 多个任职路径的分隔符。
     */
    public static final String DEPARTMENT_SEPARATOR = ",";
    /**
     * 脱敏占位星号；单元格包含该字符则拒绝写回。
     */
    public static final String MASK_MARK = "*";
    /**
     * 工作表名称。
     */
    public static final String SHEET_NAME = "成员";
    /**
     * xlsx MIME。
     */
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /**
     * 仅接受的扩展名。
     */
    public static final String FILE_EXTENSION = ".xlsx";
    /**
     * 空白创建模板文件名。
     */
    public static final String TEMPLATE_FILENAME = "成员导入模板.xlsx";
    /**
     * 更新导出文件名。
     */
    public static final String EXPORT_FILENAME = "成员通讯录.xlsx";
    /**
     * 错误报告文件名。
     */
    public static final String ERROR_FILENAME = "成员导入错误报告.xlsx";
    /**
     * 一次性密码文件名。
     */
    public static final String PASSWORD_FILENAME = "成员初始密码.xlsx";

    private MemberWorkbookColumns() {
    }
}
