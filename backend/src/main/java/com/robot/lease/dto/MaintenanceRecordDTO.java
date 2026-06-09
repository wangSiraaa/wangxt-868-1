package com.robot.lease.dto;

import com.robot.lease.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MaintenanceRecordDTO {
    private Long id;
    private Long leaseOrderId;
    private Long robotId;

    @NotNull(message = "维修开始时间不能为空")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @PositiveOrZero(message = "停机小时数不能为负数")
    private BigDecimal downtimeHours;

    private String description;

    @PositiveOrZero(message = "维修费用不能为负数")
    private BigDecimal maintenanceCost = BigDecimal.ZERO;

    private MaintenanceStatus status = MaintenanceStatus.PENDING;
}
