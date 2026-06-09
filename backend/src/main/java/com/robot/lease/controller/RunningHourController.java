package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.RunningHourDTO;
import com.robot.lease.entity.RunningHour;
import com.robot.lease.service.RunningHourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lease-orders/{leaseOrderId}/running-hours")
@RequiredArgsConstructor
public class RunningHourController {

    private final RunningHourService runningHourService;

    @GetMapping
    public Result<List<RunningHour>> list(@PathVariable Long leaseOrderId) {
        return Result.success(runningHourService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/all")
    public Result<List<RunningHour>> listAll(@PathVariable Long leaseOrderId) {
        return Result.success(runningHourService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/{id}")
    public Result<RunningHour> getById(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        return Result.success(runningHourService.findById(id));
    }

    @GetMapping("/range")
    public Result<List<RunningHour>> listByDateRange(
            @PathVariable Long leaseOrderId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(runningHourService.findByDateRange(leaseOrderId, startDate, endDate));
    }

    @PostMapping
    public Result<RunningHour> create(@PathVariable Long leaseOrderId, @Valid @RequestBody RunningHourDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(runningHourService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<RunningHour> update(@PathVariable Long leaseOrderId, @PathVariable Long id,
                                      @Valid @RequestBody RunningHourDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(runningHourService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        runningHourService.delete(id);
        return Result.success();
    }

    @PostMapping("/recalculate")
    public Result<Void> recalculate(@PathVariable Long leaseOrderId) {
        runningHourService.recalculateDeductionsForLeaseOrder(leaseOrderId);
        return Result.success();
    }
}
