package com.robot.lease.entity;

import com.robot.lease.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "maintenance_record")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lease_order_id", nullable = false)
    private Long leaseOrderId;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "downtime_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal downtimeHours;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "maintenance_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal maintenanceCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaintenanceStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
