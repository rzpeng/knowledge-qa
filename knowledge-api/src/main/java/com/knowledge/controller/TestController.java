package com.knowledge.controller;

import com.knowledge.service.ZhipuApiTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final ZhipuApiTestService testService;

    @GetMapping("/zhipu-batch")
    public String testZhipuBatch() {
        testService.testBatchSize();
        return "测试完成，请查看控制台日志";
    }
}
