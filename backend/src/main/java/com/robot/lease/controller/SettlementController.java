package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.entity.Settlement;
import com.robot.lease.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lease-orders/{leaseOrderId}/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public Result<List<Settlement>> list(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/latest")
    public Result<Settlement> getLatest(@PathVariable Long leaseOrderId) {
        List<Settlement> list = settlementService.findByLeaseOrderId(leaseOrderId);
        return Result.success(list.isEmpty() ? null : list.get(0));
    }

    @PostMapping("/calculate")
    public Result<Settlement> calculate(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.calculate(leaseOrderId));
    }

    @PostMapping("/review")
    public Result<Settlement> review(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.review(leaseOrderId));
    }

    @PostMapping("/confirm")
    public Result<Settlement> confirm(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.confirm(leaseOrderId));
    }

    @PostMapping("/cancel-confirm")
    public Result<Settlement> cancelConfirm(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.cancelConfirm(leaseOrderId));
    }
}
