package com.robot.lease.repository;

import com.robot.lease.entity.RunningHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunningHourRepository extends JpaRepository<RunningHour, Long> {
    List<RunningHour> findByLeaseOrderIdOrderByReportDateAsc(Long leaseOrderId);
    Optional<RunningHour> findByLeaseOrderIdAndReportDate(Long leaseOrderId, LocalDate reportDate);

    @Query("SELECT COALESCE(SUM(rh.billableHours), 0) FROM RunningHour rh WHERE rh.leaseOrderId = :leaseOrderId")
    BigDecimal sumBillableHoursByLeaseOrderId(@Param("leaseOrderId") Long leaseOrderId);

    @Query("SELECT rh FROM RunningHour rh WHERE rh.leaseOrderId = :leaseOrderId " +
           "AND rh.reportDate BETWEEN :startDate AND :endDate ORDER BY rh.reportDate ASC")
    List<RunningHour> findByLeaseOrderIdAndDateRange(
            @Param("leaseOrderId") Long leaseOrderId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
