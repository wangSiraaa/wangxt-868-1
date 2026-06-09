package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.MaintenanceRecordDTO;
import com.robot.lease.entity.MaintenanceRecord;
import com.robot.lease.enums.MaintenanceStatus;
import com.robot.lease.service.MaintenanceRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lease-orders/{leaseOrderId}/maintenance")
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    @GetMapping
    public Result<List<MaintenanceRecord>> list(@PathVariable Long leaseOrderId) {
        return Result.success(maintenanceRecordService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/all")
    public Result<List<MaintenanceRecord>> listAll(@PathVariable Long leaseOrderId) {
        return Result.success(maintenanceRecordService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/{id}")
    public Result<MaintenanceRecord> getById(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        return Result.success(maintenanceRecordService.findById(id));
    }

    @PostMapping
    public Result<MaintenanceRecord> create(@PathVariable Long leaseOrderId,
                                            @Valid @RequestBody MaintenanceRecordDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(maintenanceRecordService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<MaintenanceRecord> update(@PathVariable Long leaseOrderId, @PathVariable Long id,
                                            @Valid @RequestBody MaintenanceRecordDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(maintenanceRecordService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        maintenanceRecordService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/status")
    public Result<MaintenanceRecord> updateStatus(@PathVariable Long leaseOrderId, @PathVariable Long id,
                                                  @RequestParam MaintenanceStatus status) {
        return Result.success(maintenanceRecordService.updateStatus(id, status));
    }
}
