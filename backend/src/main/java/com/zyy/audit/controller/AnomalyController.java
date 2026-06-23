package com.zyy.audit.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.audit.model.vo.AnomalyEvent;
import com.zyy.audit.model.vo.AnomalySummary;
import com.zyy.audit.service.AnomalyDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for audit anomaly detection and management.
 */
@Slf4j
@RestController
@RequestMapping("/api/audit/anomalies")
@RequiredArgsConstructor
@Tag(name = "Audit Anomaly Detection", description = "Anomaly detection and monitoring APIs")
public class AnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    @Operation(summary = "List detected anomalies", description = "Retrieve paginated list of detected anomalies")
    public Result<PageVO<AnomalyEvent>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Filter by anomaly type") @RequestParam(required = false) String anomalyType,
            @Parameter(description = "Filter by severity") @RequestParam(required = false) String severity) {
        List<AnomalyEvent> anomalies = anomalyDetectionService.detectAnomalies();

        // Apply filters
        if (anomalyType != null && !anomalyType.isBlank()) {
            anomalies = anomalies.stream()
                    .filter(a -> anomalyType.equals(a.getAnomalyType()))
                    .toList();
        }
        if (severity != null && !severity.isBlank()) {
            anomalies = anomalies.stream()
                    .filter(a -> severity.equals(a.getSeverity()))
                    .toList();
        }

        // Paginate
        long total = anomalies.size();
        long pageNum = pageParam.getPageNum();
        long pageSize = pageParam.getPageSize();
        long fromIndex = (pageNum - 1) * pageSize;
        long toIndex = Math.min(fromIndex + pageSize, total);

        List<AnomalyEvent> pageItems = fromIndex < total
                ? anomalies.subList((int) fromIndex, (int) toIndex)
                : List.of();

        PageVO<AnomalyEvent> page = PageVO.<AnomalyEvent>builder()
                .items(pageItems)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .pages((total + pageSize - 1) / pageSize)
                .isFirst(pageNum == 1)
                .isLast(pageNum >= (total + pageSize - 1) / pageSize)
                .hasNext(pageNum < (total + pageSize - 1) / pageSize)
                .hasPrevious(pageNum > 1)
                .build();

        return Result.ok(page);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get anomaly summary", description = "Retrieve counts of anomalies by type and severity")
    public Result<AnomalySummary> getSummary() {
        AnomalySummary summary = anomalyDetectionService.getAnomalySummary();
        return Result.ok(summary);
    }

    @PostMapping("/scan")
    @Operation(summary = "Trigger anomaly scan", description = "Manually trigger an anomaly detection scan")
    public Result<List<AnomalyEvent>> scan() {
        List<AnomalyEvent> anomalies = anomalyDetectionService.detectAnomalies();
        return Result.ok(anomalies, "Anomaly scan completed successfully");
    }
}
