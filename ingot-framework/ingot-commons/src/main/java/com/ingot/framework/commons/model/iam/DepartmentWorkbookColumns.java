package com.ingot.framework.commons.model.iam;

/**
 * <p>集中部门表格列名、分隔符与 MIME，禁止调用点裸写表头字面量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DepartmentWorkbookColumns {
    /**
     * 更新导出与回写使用的部门 ID 列。
     */
    public static final String DEPARTMENT_ID = "部门 ID";
    /**
     * 部门名称列，兼作表头定位标志。
     */
    public static final String NAME = "部门名称";
    /**
     * 上级部门路径列。
     */
    public static final String PARENT = "上级部门";
    /**
     * 同级排序列。
     */
    public static final String SORT_ORDER = "排序";
    /**
     * 错误报告的 Excel 行号列。
     */
    public static final String ROW_NUMBER = "行号";
    /**
     * 错误报告的失败原因列。
     */
    public static final String REASON = "原因";
    /**
     * 部门路径从根到自身的分层符。
     */
    public static final String PATH_SEPARATOR = "/";
    /**
     * 工作表名称。
     */
    public static final String SHEET_NAME = "部门";
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
    public static final String TEMPLATE_FILENAME = "部门导入模板.xlsx";
    /**
     * 更新导出文件名。
     */
    public static final String EXPORT_FILENAME = "部门信息.xlsx";
    /**
     * 错误报告文件名。
     */
    public static final String ERROR_FILENAME = "部门导入错误报告.xlsx";

    private DepartmentWorkbookColumns() {
    }
}
