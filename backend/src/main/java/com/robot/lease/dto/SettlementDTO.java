package com.robot.lease.dto;

import com.robot.lease.enums.SettlementStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettlementDTO {
    private Long id;
    private String settlementNo;
    private Long leaseOrderId;
    private BigDecimal totalBillableHours;
    private BigDecimal baseRent;
    private BigDecimal maintenanceTotal;
    private BigDecimal totalAmount;
    private SettlementStatus status;
    private LocalDateTime reviewTime;
    private LocalDateTime confirmTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
