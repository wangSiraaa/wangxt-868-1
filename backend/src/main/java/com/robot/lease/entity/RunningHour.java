package com.robot.lease.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "running_hour", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lease_order_id", "report_date"})
})
public class RunningHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lease_order_id", nullable = false)
    private Long leaseOrderId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "reported_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal reportedHours;

    @Column(name = "deduction_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal deductionHours = BigDecimal.ZERO;

    @Column(name = "billable_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal billableHours;

    @Column(name = "remark", length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void calculateBillableHours() {
        if (this.reportedHours == null) {
            this.reportedHours = BigDecimal.ZERO;
        }
        if (this.deductionHours == null) {
            this.deductionHours = BigDecimal.ZERO;
        }
        BigDecimal result = this.reportedHours.subtract(this.deductionHours);
        this.billableHours = result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }
}
