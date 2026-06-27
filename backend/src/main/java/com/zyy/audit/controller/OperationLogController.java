package com.zyy.audit.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.audit.model.vo.OperationLogVO;
import com.zyy.audit.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * RESTful controller for operation log queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log Management", description = "Operation audit log query and management APIs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @Operation(summary = "List logs", description = "Retrieve paginated list of operation logs with optional filters")
    public Result<PageVO<OperationLogVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Module filter (partial match)") @RequestParam(required = false) String module,
            @Parameter(description = "Operator name filter (partial match)") @RequestParam(required = false) String operator,
            @Parameter(description = "Start time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time filter") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        PageVO<OperationLogVO> page = operationLogService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(),
                module, operator, startTime, endTime);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get log detail", description = "Retrieve operation log detail by ID")
    public Result<OperationLogVO> getById(
            @Parameter(description = "Log ID") @PathVariable Long id) {
        OperationLogVO log = operationLogService.getById(id);
        return Result.ok(log);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete log", description = "Delete an operation log (admin only)")
    @PreAuthorize("hasAuthority('audit:log:delete')")
    public Result<Void> delete(
            @Parameter(description = "Log ID") @PathVariable Long id) {
        operationLogService.delete(id);
        return Result.ok(null, "Log deleted successfully");
    }
}
