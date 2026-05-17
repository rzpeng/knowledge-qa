package com.knowledge.controller;

import com.knowledge.eval.EvalReport;
import com.knowledge.eval.EvalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;

    @PostMapping("/retrieval")
    public ResponseEntity<EvalReport> runRetrievalEval() {
        EvalReport report = evalService.evaluate();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "ready",
                "description", "POST /api/eval/retrieval to run evaluation"
        ));
    }
}
