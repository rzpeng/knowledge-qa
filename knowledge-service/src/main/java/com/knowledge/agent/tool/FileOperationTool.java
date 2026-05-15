package com.knowledge.agent.tool;

import com.knowledge.agent.config.AgentProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileOperationTool implements Tool {

    private final ToolRegistry toolRegistry;
    private final AgentProperties agentProperties;

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() {
        return "file_operation";
    }

    @Override
    public String description() {
        return "文件读写操作，支持读取文件内容(read)、写入文件(write)、列出目录文件(list)。所有操作在安全沙箱目录内执行。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("operation")
                        .description("操作类型：read（读取文件）、write（写入文件）、list（列出目录）")
                        .type(ParamType.STRING)
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("path")
                        .description("文件路径（相对于沙箱根目录），如：test.txt、data/report.json")
                        .type(ParamType.STRING)
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("content")
                        .description("写入内容（仅在write操作时使用）")
                        .type(ParamType.STRING)
                        .required(false)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String operation = (String) args.get("operation");
            String path = (String) args.get("path");

            Path baseDir = Paths.get(agentProperties.getFileBasePath()).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            Path targetPath = baseDir.resolve(path).normalize();
            if (!targetPath.startsWith(baseDir)) {
                return ToolResult.fail("路径不允许越界访问");
            }

            return switch (operation) {
                case "read" -> readFile(targetPath);
                case "write" -> {
                    String content = (String) args.get("content");
                    if (content == null) {
                        yield ToolResult.fail("写入操作需要提供content参数");
                    }
                    yield writeFile(targetPath, content);
                }
                case "list" -> listFiles(targetPath);
                default -> ToolResult.fail("不支持的操作: " + operation);
            };
        } catch (Exception e) {
            return ToolResult.fail("文件操作失败: " + e.getMessage());
        }
    }

    private ToolResult readFile(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ToolResult.fail("文件不存在: " + path.getFileName());
        }
        String content = Files.readString(path);
        return ToolResult.ok(Map.of("path", path.toString(), "content", content, "size", content.length()));
    }

    private ToolResult writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return ToolResult.ok(Map.of("path", path.toString(), "size", content.length(), "status", "written"));
    }

    private ToolResult listFiles(Path path) throws IOException {
        if (!Files.exists(path)) {
            return ToolResult.fail("路径不存在: " + path.getFileName());
        }
        List<Map<String, Object>> files;
        try (Stream<Path> stream = Files.list(path)) {
            files = stream.map(p -> {
                try {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", p.getFileName().toString());
                    info.put("type", Files.isDirectory(p) ? "directory" : "file");
                    info.put("size", Files.size(p));
                    return info;
                } catch (IOException e) {
                    Map<String, Object> errorInfo = new LinkedHashMap<>();
                    errorInfo.put("name", p.getFileName().toString());
                    errorInfo.put("type", "unknown");
                    errorInfo.put("size", 0L);
                    return errorInfo;
                }
            }).collect(Collectors.toList());
        }
        return ToolResult.ok(Map.of("path", path.toString(), "files", files, "total", files.size()));
    }
}
