package com.zyy.inventory.controller;

import com.zyy.common.Result;
import com.zyy.inventory.model.vo.RestockSuggestionVO;
import com.zyy.inventory.service.PredictiveRestockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for predictive inventory restock suggestions.
 */
@Slf4j
@RestController
@RequestMapping("/api/consumables")
@RequiredArgsConstructor
@Tag(name = "Predictive Restock", description = "Predictive restock suggestion APIs for consumable inventory")
public class PredictiveController {

    private final PredictiveRestockService predictiveRestockService;

    @GetMapping("/{id}/restock-suggestion")
    @Operation(summary = "Get restock suggestion", description = "Calculate and return a restock suggestion for a specific consumable based on consumption trends")
    public Result<RestockSuggestionVO> getRestockSuggestion(
            @Parameter(description = "Consumable ID") @PathVariable Long id) {
        RestockSuggestionVO suggestion = predictiveRestockService.calculateRestockSuggestion(id);
        return Result.ok(suggestion);
    }

    @GetMapping("/restock-suggestions")
    @Operation(summary = "Get all restock suggestions", description = "Calculate and return restock suggestions for all active consumables")
    public Result<List<RestockSuggestionVO>> getAllRestockSuggestions() {
        List<RestockSuggestionVO> suggestions = predictiveRestockService.getAllRestockSuggestions();
        return Result.ok(suggestions);
    }
}
