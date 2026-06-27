package com.zyy.asset.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.asset.model.dto.InspectionSaveDTO;
import com.zyy.asset.model.dto.InspectionUpdateDTO;
import com.zyy.asset.model.vo.InspectionVO;
import com.zyy.asset.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for equipment inspection management.
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment/inspections")
@RequiredArgsConstructor
@Tag(name = "Equipment Inspection", description = "Equipment inspection record management")
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    @Operation(summary = "Create inspection", description = "Record a new equipment inspection")
    public Result<InspectionVO> save(
            @Valid @RequestBody InspectionSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        InspectionVO vo = inspectionService.save(saveDTO, operatorId);
        return Result.ok(vo, "Inspection created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inspection", description = "Update an existing inspection record")
    public Result<InspectionVO> update(
            @Parameter(description = "Inspection ID") @PathVariable Long id,
            @Valid @RequestBody InspectionUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        InspectionVO vo = inspectionService.update(updateDTO, operatorId);
        return Result.ok(vo, "Inspection updated successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inspection by ID")
    public Result<InspectionVO> getById(
            @Parameter(description = "Inspection ID") @PathVariable Long id) {
        InspectionVO vo = inspectionService.getById(id);
        return Result.ok(vo);
    }

    @GetMapping
    @Operation(summary = "List inspections", description = "Paginated inspection list with optional filters")
    public Result<PageVO<InspectionVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Equipment ID filter") @RequestParam(required = false) Long equipmentId,
            @Parameter(description = "Result filter: PASS/FAIL/CONDITIONAL") @RequestParam(required = false) String result) {
        PageVO<InspectionVO> page = inspectionService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), equipmentId, result);
        return Result.ok(page);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inspection", description = "Soft delete an inspection record")
    public Result<Void> delete(
            @Parameter(description = "Inspection ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        inspectionService.delete(id, operatorId);
        return Result.ok(null, "Inspection deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
