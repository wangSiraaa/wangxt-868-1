package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.entity.Robot;
import com.robot.lease.entity.Settlement;
import com.robot.lease.enums.LeaseOrderStatus;
import com.robot.lease.enums.RobotStatus;
import com.robot.lease.enums.SettlementStatus;
import com.robot.lease.repository.LeaseOrderRepository;
import com.robot.lease.repository.RobotRepository;
import com.robot.lease.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RobotRepository robotRepository;
    private final LeaseOrderRepository leaseOrderRepository;
    private final SettlementRepository settlementRepository;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Robot> robots = robotRepository.findAll();
        stats.put("totalRobots", robots.size());
        stats.put("activeRobots", (int) robots.stream().filter(r -> r.getStatus() == RobotStatus.RENTED).count());
        stats.put("idleRobots", (int) robots.stream().filter(r -> r.getStatus() == RobotStatus.IDLE).count());
        stats.put("maintenanceRobots", (int) robots.stream().filter(r -> r.getStatus() == RobotStatus.MAINTENANCE).count());

        stats.put("activeOrders", leaseOrderRepository.findByStatus(LeaseOrderStatus.ACTIVE).size());
        stats.put("completedOrders", leaseOrderRepository.findByStatus(LeaseOrderStatus.COMPLETED).size());
        stats.put("settledOrders", leaseOrderRepository.findByStatus(LeaseOrderStatus.SETTLED).size());

        List<Settlement> confirmedSettlements = settlementRepository.findByStatus(SettlementStatus.CONFIRMED);
        BigDecimal totalRevenue = confirmedSettlements.stream()
                .map(Settlement::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        return Result.success(stats);
    }
}
