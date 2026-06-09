package com.robot.lease.repository;

import com.robot.lease.entity.MaintenanceRecord;
import com.robot.lease.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByLeaseOrderIdOrderByStartTimeAsc(Long leaseOrderId);
    List<MaintenanceRecord> findByLeaseOrderIdAndStatus(Long leaseOrderId, MaintenanceStatus status);

    @Query("SELECT COALESCE(SUM(m.maintenanceCost), 0) FROM MaintenanceRecord m WHERE m.leaseOrderId = :leaseOrderId")
    BigDecimal sumMaintenanceCostByLeaseOrderId(@Param("leaseOrderId") Long leaseOrderId);

    @Query("SELECT COALESCE(SUM(m.downtimeHours), 0) FROM MaintenanceRecord m " +
           "WHERE m.leaseOrderId = :leaseOrderId " +
           "AND CAST(m.startTime AS LocalDate) <= :reportDate " +
           "AND (m.endTime IS NULL OR CAST(m.endTime AS LocalDate) >= :reportDate)")
    BigDecimal sumDeductionHoursForDate(
            @Param("leaseOrderId") Long leaseOrderId,
            @Param("reportDate") LocalDate reportDate);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.leaseOrderId = :leaseOrderId " +
           "AND ((m.startTime <= :endTime AND (m.endTime IS NULL OR m.endTime >= :startTime)) " +
           "OR (CAST(m.startTime AS LocalDate) BETWEEN :startDate AND :endDate) " +
           "OR (m.endTime IS NOT NULL AND CAST(m.endTime AS LocalDate) BETWEEN :startDate AND :endDate))")
    List<MaintenanceRecord> findMaintenanceInDateRange(
            @Param("leaseOrderId") Long leaseOrderId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
