package com.robot.lease.entity;

import com.robot.lease.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_no", unique = true, nullable = false, length = 50)
    private String settlementNo;

    @Column(name = "lease_order_id", nullable = false)
    private Long leaseOrderId;

    @Column(name = "total_billable_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBillableHours = BigDecimal.ZERO;

    @Column(name = "base_rent", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseRent = BigDecimal.ZERO;

    @Column(name = "maintenance_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal maintenanceTotal = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "remark", length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
