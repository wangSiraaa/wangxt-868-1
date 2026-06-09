package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.LeaseOrderDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.service.LeaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lease-orders")
@RequiredArgsConstructor
public class LeaseOrderController {

    private final LeaseOrderService leaseOrderService;

    @GetMapping
    public Result<Page<LeaseOrder>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Result.success(leaseOrderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public Result<LeaseOrder> getById(@PathVariable Long id) {
        return Result.success(leaseOrderService.findById(id));
    }

    @PostMapping
    public Result<LeaseOrder> create(@Valid @RequestBody LeaseOrderDTO dto) {
        return Result.success(leaseOrderService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<LeaseOrder> update(@PathVariable Long id, @Valid @RequestBody LeaseOrderDTO dto) {
        return Result.success(leaseOrderService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        leaseOrderService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/activate")
    public Result<LeaseOrder> activate(@PathVariable Long id) {
        return Result.success(leaseOrderService.activate(id));
    }

    @PostMapping("/{id}/complete")
    public Result<LeaseOrder> complete(@PathVariable Long id) {
        return Result.success(leaseOrderService.complete(id));
    }

    @GetMapping("/{id}/settlement-status")
    public Result<Boolean> getSettlementStatus(@PathVariable Long id) {
        return Result.success(leaseOrderService.isSettlementConfirmed(id));
    }
}
