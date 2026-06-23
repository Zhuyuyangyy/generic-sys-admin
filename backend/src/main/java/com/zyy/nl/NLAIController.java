package com.zyy.nl;

import com.zyy.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nl")
@RequiredArgsConstructor
@Tag(name = "NL Services", description = "自然语言处理与因果检查服务")
public class NLAIController {

    private final NLService nlService;

    @PostMapping("/execute-with-causal-check")
    @Operation(summary = "自然语言执行含因果检查", description = "解析自然语言输入，检测意图，并执行因果影响检查")
    public Result<Map<String, Object>> executeWithCausalCheck(
            @Parameter(description = "自然语言输入") @RequestBody Map<String, String> request) {

        String input = request.get("input");

        ExecuteResult result = nlService.executeWithCausalCheck(input);

        Map<String, Object> response = Map.of(
            "success", true,
            "intent", result.getIntent(),
            "entityId", result.getEntityId(),
            "causalCheckPerformed", result.isCausalCheckPerformed(),
            "impactedNodesCount", result.getImpactedNodesCount() != null ? result.getImpactedNodesCount() : 0,
            "hasHighImpact", result.isHasHighImpact(),
            "latencyMs", result.getLatencyMs()
        );

        return Result.ok(response);
    }

    @PostMapping("/parse")
    @Operation(summary = "自然语言解析", description = "仅解析自然语言输入，不执行因果检查")
    public Result<NLParseResult> parse(
            @Parameter(description = "自然语言输入") @RequestBody Map<String, String> request) {

        String input = request.get("input");
        NLParseResult result = nlService.parse(input);

        return Result.ok(result);
    }

    @PostMapping("/dry-run")
    @Operation(summary = "NL Dry Run", description = "Parse NL input and return a preview of what would happen WITHOUT executing. Returns a confirmationId for subsequent execute-confirmed call.")
    public Result<DryRunResult> dryRun(
            @Parameter(description = "自然语言输入") @RequestBody Map<String, String> request) {

        String input = request.get("input");
        DryRunResult result = nlService.dryRun(input);

        return Result.ok(result);
    }

    @PostMapping("/execute-confirmed")
    @Operation(summary = "NL Execute Confirmed", description = "Execute NL command after confirmation. Requires a valid confirmationId from a previous dry-run call.")
    public Result<ExecuteResult> executeConfirmed(
            @Parameter(description = "包含 input 和 confirmationId 的请求") @RequestBody Map<String, String> request) {

        String input = request.get("input");
        String confirmationId = request.get("confirmationId");

        ExecuteResult result = nlService.executeConfirmed(input, confirmationId);

        return Result.ok(result);
    }

    @GetMapping("/test-commands")
    @Operation(summary = "获取测试命令集", description = "返回实验用的标准测试命令集")
    public Result<java.util.List<String>> getTestCommands() {
        return Result.ok(nlService.getTestCommands());
    }
}
