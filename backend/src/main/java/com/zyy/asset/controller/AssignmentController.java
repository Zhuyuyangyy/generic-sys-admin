package com.zyy.asset.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.asset.model.dto.AssignmentSaveDTO;
import com.zyy.asset.model.vo.AssignmentVO;
import com.zyy.asset.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for equipment assignment management.
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment/assignments")
@RequiredArgsConstructor
@Tag(name = "Equipment Assignment", description = "Equipment assignment and return management")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "Assign equipment", description = "Assign equipment to a user")
    public Result<AssignmentVO> save(
            @Valid @RequestBody AssignmentSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        AssignmentVO vo = assignmentService.save(saveDTO, operatorId);
        return Result.ok(vo, "Equipment assigned successfully");
    }

    @PutMapping("/{id}/return")
    @Operation(summary = "Return equipment", description = "Record equipment return")
    public Result<AssignmentVO> returnEquipment(
            @Parameter(description = "Assignment ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        AssignmentVO vo = assignmentService.returnEquipment(id, operatorId);
        return Result.ok(vo, "Equipment returned successfully");
    }

    @GetMapping
    @Operation(summary = "List assignments", description = "Paginated assignment list with optional filters")
    public Result<PageVO<AssignmentVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Equipment ID filter") @RequestParam(required = false) Long equipmentId,
            @Parameter(description = "User ID filter") @RequestParam(required = false) Long userId,
            @Parameter(description = "Status filter: ACTIVE/RETURNED") @RequestParam(required = false) String status) {
        PageVO<AssignmentVO> page = assignmentService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), equipmentId, userId, status);
        return Result.ok(page);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete assignment", description = "Soft delete an assignment record")
    public Result<Void> delete(
            @Parameter(description = "Assignment ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        assignmentService.delete(id, operatorId);
        return Result.ok(null, "Assignment deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
