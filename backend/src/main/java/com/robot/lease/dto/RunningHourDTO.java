package com.robot.lease.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RunningHourDTO {
    private Long id;
    private Long leaseOrderId;

    @NotNull(message = "上报日期不能为空")
    private LocalDate reportDate;

    @NotNull(message = "上报小时数不能为空")
    @PositiveOrZero(message = "上报小时数不能为负数")
    private BigDecimal reportedHours;

    private BigDecimal deductionHours = BigDecimal.ZERO;
    private BigDecimal billableHours;
    private String remark;
}
