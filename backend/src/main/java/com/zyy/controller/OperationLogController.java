package com.zyy.controller;

import com.zyy.aspect.Log;
import com.zyy.common.PageParam;
import com.zyy.common.Result;
import com.zyy.enums.BusinessType;
import com.zyy.model.entity.SysOperationLogEntity;
import com.zyy.model.vo.PageVO;
import com.zyy.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志查询 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "Operation Log", description = "系统操作日志查询")
public class OperationLogController {

    private final SysOperationLogService operationLogService;

    @GetMapping
    @Operation(summary = "操作日志列表", description = "分页查询操作日志，支持多条件筛选")
    @PreAuthorize("hasAuthority('sys:log:list')")
    public Result<PageVO<SysOperationLogEntity>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "操作模块") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operation,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator,
            @Parameter(description = "状态：0=失败，1=成功") @RequestParam(required = false) String status,
            @Parameter(description = "开始日期：yyyy-MM-dd") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束日期：yyyy-MM-dd") @RequestParam(required = false) String endTime) {

        PageVO<SysOperationLogEntity> page = operationLogService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(),
                module, operation, operator, status, startTime, endTime);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "日志详情", description = "根据ID查询单条操作日志")
    @PreAuthorize("hasAuthority('sys:log:query')")
    public Result<SysOperationLogEntity> getById(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        // 日志查询一般不需要，暂不实现
        return Result.ok(null);
    }
}