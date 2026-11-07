package com.ingot.cloud.iam.application.organization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.workbook.DepartmentWorkbookColumns;
import com.ingot.cloud.iam.domain.workbook.DepartmentWorkbookLimits;
import com.ingot.cloud.iam.domain.workbook.DepartmentWorkbookReasons;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>用 EasyExcel 读写部门工作簿，保留填写须知行并按部门名称定位表头。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DepartmentWorkbookExcel {
    private static final String NOTICE_TITLE = "填写须知";
    private static final String NOTICE_HEADER = "请勿修改表头行，数据从下一行开始填写";
    private static final String NOTICE_CREATE = "部门名称和上级部门必填；上级部门填写从根部门开始的路径，用 / 分层；不能创建根部门；排序留空按 0";
    private static final String NOTICE_UPDATE = "按部门 ID 更新；名称和上级不能为空；根部门不能变更上级；同一上级下名称必须唯一";
    private static final String NOTICE_LIMIT = "数据行不超过 " + DepartmentWorkbookLimits.MAX_DATA_ROWS + " 行，仅支持 XLSX";

    private DepartmentWorkbookExcel() {
    }

    /**
     * 生成带须知的空白或导出工作簿。
     *
     * @param headers 表头
     * @param rows 数据行，键为表头
     * @param update 是否更新模板须知
     * @return xlsx 字节
     */
    public static byte[] writeDepartments(List<String> headers, List<Map<String, String>> rows, boolean update) {
        List<List<String>> sheet = new ArrayList<>();
        sheet.add(List.of(NOTICE_TITLE));
        sheet.add(List.of(NOTICE_HEADER));
        sheet.add(List.of(update ? NOTICE_UPDATE : NOTICE_CREATE));
        sheet.add(List.of(NOTICE_LIMIT));
        sheet.add(List.copyOf(headers));
        if (rows != null) {
            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    values.add(row.getOrDefault(header, ""));
                }
                sheet.add(values);
            }
        }
        return write(sheet);
    }

    /**
     * 生成错误报告。
     *
     * @param errors 行号与原因
     * @return xlsx 字节
     */
    public static byte[] writeErrors(List<DepartmentWorkbookRowFailure> errors) {
        List<List<String>> sheet = new ArrayList<>();
        sheet.add(List.of(DepartmentWorkbookColumns.ROW_NUMBER, DepartmentWorkbookColumns.REASON));
        for (DepartmentWorkbookRowFailure error : errors) {
            sheet.add(List.of(Integer.toString(error.row()), error.reason()));
        }
        return write(sheet);
    }

    /**
     * 读取第一张表并定位表头。
     *
     * @param content 文件
     * @return 表头与数据
     */
    public static DepartmentWorkbookSheet read(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.FILE_INVALID);
        }
        List<List<String>> raw = new ArrayList<>();
        try {
            EasyExcel.read(new ByteArrayInputStream(content), new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    raw.add(cells(data));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).headRowNumber(0).sheet().doRead();
        } catch (RuntimeException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.FILE_INVALID);
        }
        int headerIndex = -1;
        for (int index = 0; index < raw.size(); index++) {
            if (raw.get(index).contains(DepartmentWorkbookColumns.NAME)) {
                headerIndex = index;
                break;
            }
        }
        if (headerIndex < 0) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.HEADER_MISSING);
        }
        List<String> headers = raw.get(headerIndex);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int index = headerIndex + 1; index < raw.size(); index++) {
            List<String> values = raw.get(index);
            if (blank(values)) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                String header = headers.get(column);
                if (header == null || header.isBlank()) {
                    continue;
                }
                row.put(header, column < values.size() ? normalize(values.get(column)) : "");
            }
            rows.add(row);
        }
        if (rows.size() > DepartmentWorkbookLimits.MAX_DATA_ROWS) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.ROW_LIMIT_EXCEEDED);
        }
        return new DepartmentWorkbookSheet(headers, rows, headerIndex + 1);
    }

    static byte[] write(List<List<String>> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).sheet(DepartmentWorkbookColumns.SHEET_NAME).doWrite(rows);
        return output.toByteArray();
    }

    private static List<String> cells(Map<Integer, String> data) {
        int size = 0;
        for (Integer key : data.keySet()) {
            if (key != null) {
                size = Math.max(size, key + 1);
            }
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            values.add(normalize(data.get(index)));
        }
        return values;
    }

    private static boolean blank(List<String> values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (text.matches("\\d+\\.0+")) {
            return text.substring(0, text.indexOf('.'));
        }
        if (text.matches("\\d+(\\.\\d+)?E\\d+")) {
            return new BigDecimal(text).toPlainString();
        }
        return text;
    }

    /**
     * 已定位表头的工作簿。
     *
     * @param headers 表头
     * @param rows 数据
     * @param headerRow 表头所在 Excel 行号，从 1 开始
     */
    public record DepartmentWorkbookSheet(List<String> headers, List<Map<String, String>> rows, int headerRow) {
        /**
         * 数据行对应的 Excel 行号。
         *
         * @param index 数据下标
         * @return Excel 行号
         */
        public int excelRow(int index) {
            return headerRow + 1 + index;
        }

        /**
         * 是否包含指定列。
         *
         * @param header 表头
         * @return 包含时为 true
         */
        public boolean has(String header) {
            return headers.contains(header);
        }
    }

    /**
     * 错误报告一行。
     *
     * @param row Excel 行号
     * @param reason 失败原因
     */
    public record DepartmentWorkbookRowFailure(int row, String reason) {
    }

    /**
     * 任务结果计数。
     *
     * @param successCount 成功行
     * @param failCount 失败行
     * @param errors 失败明细
     */
    public record DepartmentWorkbookOutcome(int successCount, int failCount, List<DepartmentWorkbookRowFailure> errors) {
    }
}
