package com.knowledge.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CalculatorTool implements Tool {

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "执行数学计算，支持加(+)、减(-)、乘(*)、除(/)、幂(^或**)、括号等运算。传入数学表达式，返回计算结果。";
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
            String expression = (String) args.get("expression");
            if (!expression.matches("[0-9+\\-*/^().,%\\s]+")) {
                return ToolResult.fail("表达式包含非法字符");
            }
            String safeExpr = expression.replace("^", "**");
            Object result = engine.eval(safeExpr);
            return ToolResult.ok(expression + " = " + result);
        } catch (ScriptException e) {
            return ToolResult.fail("计算错误: " + e.getMessage());
        }
    }
}
