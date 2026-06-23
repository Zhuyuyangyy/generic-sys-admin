package com.zyy.inventory.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.inventory.model.dto.SupplierSaveDTO;
import com.zyy.inventory.model.dto.SupplierUpdateDTO;
import com.zyy.inventory.model.vo.SupplierVO;
import com.zyy.inventory.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for supplier management.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET    /api/consumables/suppliers          - Paginated list with filters</li>
 *   <li>GET    /api/consumables/suppliers/{id}     - Single supplier record</li>
 *   <li>POST   /api/consumables/suppliers          - Create supplier</li>
 *   <li>PUT    /api/consumables/suppliers/{id}     - Update supplier</li>
 *   <li>DELETE /api/consumables/suppliers/{id}     - Soft delete</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/consumables/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management", description = "Consumable supplier and vendor management")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @Operation(summary = "List suppliers", description = "Paginated supplier list with optional filters")
    public Result<PageVO<SupplierVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Supplier name filter (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Status filter: 0=inactive, 1=active") @RequestParam(required = false) Integer status) {
        PageVO<SupplierVO> page = supplierService.getPage(
                pageParam.getPageNum(), pageParam.getPageSize(), name, status);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID")
    public Result<SupplierVO> getById(
            @Parameter(description = "Supplier ID") @PathVariable Long id) {
        SupplierVO vo = supplierService.getById(id);
        return Result.ok(vo);
    }

    @PostMapping
    @Operation(summary = "Create supplier", description = "Register a new consumable supplier")
    public Result<SupplierVO> save(
            @Valid @RequestBody SupplierSaveDTO saveDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        SupplierVO vo = supplierService.save(saveDTO, operatorId);
        return Result.ok(vo, "Supplier created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier", description = "Update supplier details")
    public Result<SupplierVO> update(
            @Parameter(description = "Supplier ID") @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        SupplierVO vo = supplierService.update(updateDTO, operatorId);
        return Result.ok(vo, "Supplier updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete supplier", description = "Soft delete a supplier")
    public Result<Void> delete(
            @Parameter(description = "Supplier ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        supplierService.delete(id, operatorId);
        return Result.ok(null, "Supplier deleted");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        return 1L;
    }
}
