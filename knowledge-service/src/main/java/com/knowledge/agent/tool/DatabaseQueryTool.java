package com.knowledge.agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseQueryTool implements Tool {

    private final JdbcTemplate jdbcTemplate;

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^\\s*SELECT\\s+.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int MAX_ROWS = 100;

    @Override
    public String name() {
        return "database_query";
    }

    @Override
    public String description() {
        return "执行数据库查询，仅支持SELECT语句。返回查询结果表格。可用于查询业务数据。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("sql")
                        .description("SQL查询语句，必须为SELECT开头")
                        .type(ParamType.STRING)
                        .required(true)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String sql = ((String) args.get("sql")).trim();
            if (!SELECT_PATTERN.matcher(sql).matches()) {
                return ToolResult.fail("仅允许执行SELECT查询");
            }
            String upper = sql.toUpperCase();
            if (upper.contains("INTO OUTFILE") || upper.contains("INTO DUMPFILE")
                    || upper.contains("LOAD_FILE") || upper.contains("SLEEP(")) {
                return ToolResult.fail("查询包含不允许的操作");
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.size() > MAX_ROWS) {
                rows = rows.subList(0, MAX_ROWS);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> safeRow = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    safeRow.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "NULL");
                }
                result.add(safeRow);
            }

            return ToolResult.ok(Map.of(
                    "columns", rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet()),
                    "rows", result,
                    "total", result.size()
            ));
        } catch (Exception e) {
            return ToolResult.fail("查询失败: " + e.getMessage());
        }
    }
}
