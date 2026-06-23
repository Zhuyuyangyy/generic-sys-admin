package com.zyy.asset.controller;

import com.zyy.common.Result;
import com.zyy.asset.model.dto.LocationSaveDTO;
import com.zyy.asset.model.dto.LocationUpdateDTO;
import com.zyy.asset.model.vo.LocationVO;
import com.zyy.asset.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful controller for asset location management.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET    /api/equipment/locations          - List locations as tree</li>
 *   <li>POST   /api/equipment/locations          - Create location</li>
 *   <li>PUT    /api/equipment/locations/{id}     - Update location</li>
 *   <li>DELETE /api/equipment/locations/{id}     - Delete location</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment/locations")
@RequiredArgsConstructor
@Tag(name = "Location Management", description = "Asset location hierarchy management")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "List locations", description = "Get all locations as a tree structure")
    public Result<List<LocationVO>> getTree() {
        List<LocationVO> tree = locationService.getTree();
        return Result.ok(tree);
    }

    @PostMapping
    @Operation(summary = "Create location", description = "Create a new asset location")
    public Result<LocationVO> save(
            @Valid @RequestBody LocationSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        LocationVO vo = locationService.save(saveDTO, operatorId);
        return Result.ok(vo, "Location created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update location", description = "Update location details")
    public Result<LocationVO> update(
            @Parameter(description = "Location ID") @PathVariable Long id,
            @Valid @RequestBody LocationUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        LocationVO vo = locationService.update(updateDTO, operatorId);
        return Result.ok(vo, "Location updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete location", description = "Delete a location (fails if children exist)")
    public Result<Void> delete(
            @Parameter(description = "Location ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        locationService.delete(id, operatorId);
        return Result.ok(null, "Location deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
