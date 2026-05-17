package com.knowledge.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "执行数学计算，支持加(+)、减(-)、乘(*)、除(/)、幂(^)、括号等运算。传入数学表达式，返回计算结果。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("expression")
                        .description("数学表达式，如：25 + 10 * 2")
                        .type(ParamType.STRING)
                        .required(true)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String expression = ((String) args.get("expression")).trim();
            if (!expression.matches("[0-9+\\-*/^().%\\s]+")) {
                return ToolResult.fail("表达式包含非法字符");
            }
            double result = eval(expression);
            return ToolResult.ok(expression + " = " + (result == (long) result ? String.valueOf((long) result) : String.valueOf(result)));
        } catch (Exception e) {
            return ToolResult.fail("计算错误: " + e.getMessage());
        }
    }

    // ---- 递归下降解析器 ----
    private int pos = -1;
    private int ch;
    private String expr;

    private double eval(String s) {
        pos = -1;
        expr = s;
        next();
        double result = parseExpression();
        return result;
    }

    private void next() {
        ch = ++pos < expr.length() ? expr.charAt(pos) : -1;
    }

    private boolean eat(int c) {
        while (ch == ' ') next();
        if (ch == c) { next(); return true; }
        return false;
    }

    // expression = term ( '+' | '-' term )*
    private double parseExpression() {
        double result = parseTerm();
        while (true) {
            if (eat('+')) result += parseTerm();
            else if (eat('-')) result -= parseTerm();
            else return result;
        }
    }

    // term = factor ( '*' | '/' factor )*
    private double parseTerm() {
        double result = parseFactor();
        while (true) {
            if (eat('*')) result *= parseFactor();
            else if (eat('/')) result /= parseFactor();
            else return result;
        }
    }

    // factor = power ( '^' factor )*
    private double parseFactor() {
        double result = parsePower();
        if (eat('^')) result = Math.pow(result, parseFactor());
        return result;
    }

    // power = unary (no power op yet, handles unary + and -)
    private double parsePower() {
        if (eat('+')) return parsePower();
        if (eat('-')) return -parsePower();
        // parentheses
        if (eat('(')) {
            double result = parseExpression();
            eat(')');
            return result;
        }
        // number
        int start = pos;
        while ((ch >= '0' && ch <= '9') || ch == '.') next();
        return Double.parseDouble(expr.substring(start, pos));
    }
}
