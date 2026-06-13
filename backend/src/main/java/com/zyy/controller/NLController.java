package com.zyy.controller;

import com.zyy.nl.CausalDAGService;
import com.zyy.nl.NLService;
import com.zyy.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自然语言业务流控制器。
 * <p>提供NL指令执行（含因果预测）REST接口。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@RestController
@RequestMapping("/api/nl")
@RequiredArgsConstructor
@Tag(name = "自然语言业务流")
public class NLController {

    private final NLService nlService;
    private final CausalDAGService causalDAGService;

    @PostMapping("/execute")
    @Operation(summary = "执行自然语言指令（标准）")
    public Result<String> execute(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        if (input == null || input.isBlank()) {
            return Result.fail("input不能为空");
        }
        String result = nlService.execute(input);
        return Result.success(result);
    }

    /**
     * 核心接口——因果感知NL执行。
     * DELETE/UPDATE操作前自动预测下游影响范围，返回执行结果+影响节点列表。
     */
    @PostMapping("/execute-with-causal-check")
    @Operation(summary = "执行NL指令并预测因果影响（专利核心证据接口）")
    public Result<NLService.CausalCheckResult> executeWithCausalCheck(
            @RequestBody Map<String, String> request) {
        String input = request.get("input");
        if (input == null || input.isBlank()) {
            return Result.fail("input不能为空");
        }
        NLService.CausalCheckResult result = nlService.executeWithCausalCheck(input);
        return Result.success(result);
    }

    /**
     * 诊断接口——返回系统因果图信息。
     */
    @GetMapping("/causal/dag")
    @Operation(summary = "获取系统因果图拓扑顺序（诊断用）")
    public Result<Map<String, Object>> getDAGInfo() {
        return Result.success(Map.of(
                "nodeCount", causalDAGService.getNodeCount(),
                "topologicalOrder", causalDAGService.getTopologicalOrder(),
                "decayFactor", 0.8
        ));
    }
}