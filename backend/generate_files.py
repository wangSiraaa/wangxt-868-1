#!/usr/bin/env python3
import os

BASE_DIR = '/Users/mingyuan/workspace/sihuo/wangxtw3/868/backend/src/main/java/com/robot/lease'

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(content)
    print(f'Created: {os.path.relpath(path, BASE_DIR)}')

# Repository layer
repos = {
    'RobotRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RobotRepository extends JpaRepository<Robot, Long> {
    Optional<Robot> findByCode(String code);
    boolean existsByCode(String code);
}
''',
    'LeaseOrderRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.LeaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaseOrderRepository extends JpaRepository<LeaseOrder, Long> {
    Optional<LeaseOrder> findByOrderNo(String orderNo);
    boolean existsByOrderNo(String orderNo);
}
''',
    'RunningHourRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.RunningHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunningHourRepository extends JpaRepository<RunningHour, Long> {
    List<RunningHour> findByLeaseOrderId(Long leaseOrderId);
    List<RunningHour> findByRobotId(Long robotId);
    Optional<RunningHour> findByLeaseOrderIdAndRobotIdAndReportDate(Long leaseOrderId, Long robotId, LocalDate reportDate);
    
    @Query("SELECT rh FROM RunningHour rh WHERE rh.leaseOrderId = :leaseOrderId AND rh.robotId = :robotId AND rh.reportDate BETWEEN :startDate AND :endDate")
    List<RunningHour> findByDateRange(
            @Param("leaseOrderId") Long leaseOrderId,
            @Param("robotId") Long robotId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
''',
    'MaintenanceRecordRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.MaintenanceRecord;
import com.robot.lease.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByLeaseOrderId(Long leaseOrderId);
    List<MaintenanceRecord> findByRobotId(Long robotId);
    List<MaintenanceRecord> findByLeaseOrderIdAndStatus(Long leaseOrderId, MaintenanceStatus status);
    
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.leaseOrderId = :leaseOrderId AND m.robotId = :robotId AND m.endTime >= :startTime AND m.startTime <= :endTime")
    List<MaintenanceRecord> findOverlappingRecords(
            @Param("leaseOrderId") Long leaseOrderId,
            @Param("robotId") Long robotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
''',
    'SettlementRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.Settlement;
import com.robot.lease.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementNo(String settlementNo);
    boolean existsBySettlementNo(String settlementNo);
    List<Settlement> findByLeaseOrderId(Long leaseOrderId);
    List<Settlement> findByLeaseOrderIdAndStatus(Long leaseOrderId, SettlementStatus status);
}
'''
}

print('=== Creating Repository files ===')
for name, content in repos.items():
    write_file(os.path.join(BASE_DIR, 'repository', name), content)

# Common class
common = {
    'Result.java': '''package com.robot.lease.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}
''',
    'BusinessException.java': '''package com.robot.lease.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
'''
}

print('=== Creating Common files ===')
for name, content in common.items():
    write_file(os.path.join(BASE_DIR, 'common', name), content)

print('All done!')
