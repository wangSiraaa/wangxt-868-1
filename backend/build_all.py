#!/usr/bin/env python3
import os

BASE_DIR = '/Users/mingyuan/workspace/sihuo/wangxtw3/868/backend/src/main/java/com/robot/lease'
RESOURCE_DIR = '/Users/mingyuan/workspace/sihuo/wangxtw3/868/backend/src/main/resources'
ROOT_DIR = '/Users/mingyuan/workspace/sihuo/wangxtw3/868/backend'

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Created: {path}')

# ============ Enums ============
enums = {
    'RobotStatus.java': '''package com.robot.lease.enums;

public enum RobotStatus {
    IDLE,
    RENTED,
    MAINTENANCE,
    SCRAPPED
}
''',
    'LeaseOrderStatus.java': '''package com.robot.lease.enums;

public enum LeaseOrderStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    SETTLED
}
''',
    'MaintenanceStatus.java': '''package com.robot.lease.enums;

public enum MaintenanceStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
''',
    'SettlementStatus.java': '''package com.robot.lease.enums;

public enum SettlementStatus {
    DRAFT,
    REVIEWED,
    CONFIRMED
}
'''
}

print('=== Creating Enums ===')
for name, content in enums.items():
    write_file(os.path.join(BASE_DIR, 'enums', name), content)

# ============ Entities ============
entities = {
    'Robot.java': '''package com.robot.lease.entity;

import com.robot.lease.enums.RobotStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "robot")
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RobotStatus status;

    @Column(name = "hourly_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
''',
    'LeaseOrder.java': '''package com.robot.lease.entity;

import com.robot.lease.enums.LeaseOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lease_order")
public class LeaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, nullable = false, length = 50)
    private String orderNo;

    @Column(name = "lessee_factory", nullable = false, length = 200)
    private String lesseeFactory;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "remark", length = 500)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaseOrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
''',
    'RunningHour.java': '''package com.robot.lease.entity;

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
''',
    'MaintenanceRecord.java': '''package com.robot.lease.entity;

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
''',
    'Settlement.java': '''package com.robot.lease.entity;

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
'''
}

print('=== Creating Entities ===')
for name, content in entities.items():
    write_file(os.path.join(BASE_DIR, 'entity', name), content)

# ============ Repositories ============
repositories = {
    'RobotRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.Robot;
import com.robot.lease.enums.RobotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RobotRepository extends JpaRepository<Robot, Long> {
    Optional<Robot> findByCode(String code);
    boolean existsByCode(String code);
    List<Robot> findByStatus(RobotStatus status);
}
''',
    'LeaseOrderRepository.java': '''package com.robot.lease.repository;

import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.enums.LeaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaseOrderRepository extends JpaRepository<LeaseOrder, Long> {
    Optional<LeaseOrder> findByOrderNo(String orderNo);
    boolean existsByOrderNo(String orderNo);
    List<LeaseOrder> findByStatus(LeaseOrderStatus status);
    List<LeaseOrder> findByRobotId(Long robotId);
}
''',
    'RunningHourRepository.java': '''package com.robot.lease.repository;

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
''',
    'MaintenanceRecordRepository.java': '''package com.robot.lease.repository;

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
    List<Settlement> findByLeaseOrderIdOrderByCreatedAtDesc(Long leaseOrderId);
    Optional<Settlement> findFirstByLeaseOrderIdOrderByCreatedAtDesc(Long leaseOrderId);
    List<Settlement> findByStatus(SettlementStatus status);
}
'''
}

print('=== Creating Repositories ===')
for name, content in repositories.items():
    write_file(os.path.join(BASE_DIR, 'repository', name), content)

# ============ Common classes ============
commons = {
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
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
''',
    'GlobalExceptionHandler.java': '''package com.robot.lease.common;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Result<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统内部错误: " + e.getMessage()));
    }
}
'''
}

print('=== Creating Common classes ===')
for name, content in commons.items():
    write_file(os.path.join(BASE_DIR, 'common', name), content)

# ============ DTOs ============
dtos = {
    'RobotDTO.java': '''package com.robot.lease.dto;

import com.robot.lease.enums.RobotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RobotDTO {
    private Long id;

    @NotBlank(message = "设备编码不能为空")
    private String robotCode;

    @NotBlank(message = "设备名称不能为空")
    private String robotName;

    private String model;
    private String manufacturer;

    @NotNull(message = "小时费率不能为空")
    @Positive(message = "小时费率必须为正数")
    private BigDecimal hourlyRate;

    private RobotStatus status = RobotStatus.IDLE;
}
''',
    'LeaseOrderDTO.java': '''package com.robot.lease.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.robot.lease.enums.LeaseOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeaseOrderDTO {
    private Long id;
    private String orderNo;

    @NotBlank(message = "承租工厂不能为空")
    private String lesseeFactory;

    private String contactPerson;
    private String contactPhone;

    @NotNull(message = "机器人ID不能为空")
    private Long robotId;

    @NotNull(message = "租赁开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String remark;
    private LeaseOrderStatus status = LeaseOrderStatus.DRAFT;
}
''',
    'RunningHourDTO.java': '''package com.robot.lease.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RunningHourDTO {
    private Long id;

    @NotNull(message = "租赁单ID不能为空")
    private Long leaseOrderId;

    @NotNull(message = "上报日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @NotNull(message = "上报小时数不能为空")
    @PositiveOrZero(message = "上报小时数不能为负数")
    private BigDecimal reportedHours;

    private BigDecimal deductionHours = BigDecimal.ZERO;
    private BigDecimal billableHours;
    private String remark;
}
''',
    'MaintenanceRecordDTO.java': '''package com.robot.lease.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.robot.lease.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MaintenanceRecordDTO {
    private Long id;

    @NotNull(message = "租赁单ID不能为空")
    private Long leaseOrderId;

    private Long robotId;

    @NotNull(message = "维修开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotNull(message = "停机小时数不能为空")
    @PositiveOrZero(message = "停机小时数不能为负数")
    private BigDecimal downtimeHours;

    @NotBlank(message = "维修描述不能为空")
    private String description;

    @PositiveOrZero(message = "维修费用不能为负数")
    private BigDecimal maintenanceCost = BigDecimal.ZERO;

    private MaintenanceStatus status = MaintenanceStatus.PENDING;
}
''',
    'SettlementDTO.java': '''package com.robot.lease.dto;

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
'''
}

print('=== Creating DTOs ===')
for name, content in dtos.items():
    write_file(os.path.join(BASE_DIR, 'dto', name), content)

# ============ Services ============
services = {
    'RobotService.java': '''package com.robot.lease.service;

import com.robot.lease.dto.RobotDTO;
import com.robot.lease.entity.Robot;
import com.robot.lease.enums.RobotStatus;
import com.robot.lease.common.BusinessException;
import com.robot.lease.repository.RobotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository robotRepository;

    public Page<Robot> findAll(Pageable pageable) {
        return robotRepository.findAll(pageable);
    }

    public Robot findById(Long id) {
        return robotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "机器人设备不存在: " + id));
    }

    @Transactional
    public Robot create(RobotDTO dto) {
        if (robotRepository.existsByCode(dto.getRobotCode())) {
            throw new BusinessException("设备编码已存在: " + dto.getRobotCode());
        }
        Robot robot = new Robot();
        robot.setCode(dto.getRobotCode());
        robot.setName(dto.getRobotName());
        robot.setModel(dto.getModel());
        robot.setManufacturer(dto.getManufacturer());
        robot.setHourlyRate(dto.getHourlyRate());
        robot.setStatus(dto.getStatus() != null ? dto.getStatus() : RobotStatus.IDLE);
        return robotRepository.save(robot);
    }

    @Transactional
    public Robot update(Long id, RobotDTO dto) {
        Robot robot = findById(id);
        if (!robot.getCode().equals(dto.getRobotCode()) && robotRepository.existsByCode(dto.getRobotCode())) {
            throw new BusinessException("设备编码已存在: " + dto.getRobotCode());
        }
        robot.setCode(dto.getRobotCode());
        robot.setName(dto.getRobotName());
        robot.setModel(dto.getModel());
        robot.setManufacturer(dto.getManufacturer());
        robot.setHourlyRate(dto.getHourlyRate());
        if (dto.getStatus() != null) {
            robot.setStatus(dto.getStatus());
        }
        return robotRepository.save(robot);
    }

    @Transactional
    public void delete(Long id) {
        Robot robot = findById(id);
        if (robot.getStatus() == RobotStatus.RENTED) {
            throw new BusinessException("设备正在租赁中，无法删除");
        }
        robotRepository.delete(robot);
    }

    @Transactional
    public void updateStatus(Long id, RobotStatus status) {
        Robot robot = findById(id);
        robot.setStatus(status);
        robotRepository.save(robot);
    }
}
''',
    'LeaseOrderService.java': '''package com.robot.lease.service;

import com.robot.lease.dto.LeaseOrderDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.entity.Robot;
import com.robot.lease.entity.Settlement;
import com.robot.lease.enums.LeaseOrderStatus;
import com.robot.lease.enums.RobotStatus;
import com.robot.lease.enums.SettlementStatus;
import com.robot.lease.common.BusinessException;
import com.robot.lease.repository.LeaseOrderRepository;
import com.robot.lease.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseOrderService {

    private final LeaseOrderRepository leaseOrderRepository;
    private final RobotService robotService;
    private final SettlementRepository settlementRepository;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicInteger orderCounter = new AtomicInteger(0);

    public Page<LeaseOrder> findAll(Pageable pageable) {
        return leaseOrderRepository.findAll(pageable);
    }

    public LeaseOrder findById(Long id) {
        return leaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "租赁单不存在: " + id));
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FORMAT);
        int seq = orderCounter.incrementAndGet() % 10000;
        return "LO" + timestamp + String.format("%04d", seq);
    }

    @Transactional
    public LeaseOrder create(LeaseOrderDTO dto) {
        Robot robot = robotService.findById(dto.getRobotId());

        LeaseOrder order = new LeaseOrder();
        order.setOrderNo(generateOrderNo());
        order.setLesseeFactory(dto.getLesseeFactory());
        order.setContactPerson(dto.getContactPerson());
        order.setContactPhone(dto.getContactPhone());
        order.setRobotId(dto.getRobotId());
        order.setStartTime(dto.getStartTime());
        order.setEndTime(dto.getEndTime());
        order.setRemark(dto.getRemark());
        order.setStatus(LeaseOrderStatus.DRAFT);
        return leaseOrderRepository.save(order);
    }

    @Transactional
    public LeaseOrder update(Long id, LeaseOrderDTO dto) {
        LeaseOrder order = findById(id);
        checkSettlementLocked(id, "修改租赁单");

        if (!order.getRobotId().equals(dto.getRobotId())) {
            robotService.findById(dto.getRobotId());
        }
        order.setLesseeFactory(dto.getLesseeFactory());
        order.setContactPerson(dto.getContactPerson());
        order.setContactPhone(dto.getContactPhone());
        order.setRobotId(dto.getRobotId());
        order.setStartTime(dto.getStartTime());
        order.setEndTime(dto.getEndTime());
        order.setRemark(dto.getRemark());
        return leaseOrderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        LeaseOrder order = findById(id);
        if (order.getStatus() != LeaseOrderStatus.DRAFT) {
            throw new BusinessException("只能删除草稿状态的租赁单");
        }
        checkSettlementLocked(id, "删除租赁单");
        leaseOrderRepository.delete(order);
    }

    @Transactional
    public LeaseOrder activate(Long id) {
        LeaseOrder order = findById(id);
        if (order.getStatus() != LeaseOrderStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的租赁单才能激活");
        }
        Robot robot = robotService.findById(order.getRobotId());
        if (robot.getStatus() == RobotStatus.RENTED) {
            throw new BusinessException("该设备已被其他租赁单占用");
        }
        order.setStatus(LeaseOrderStatus.ACTIVE);
        robotService.updateStatus(robot.getId(), RobotStatus.RENTED);
        return leaseOrderRepository.save(order);
    }

    @Transactional
    public LeaseOrder complete(Long id) {
        LeaseOrder order = findById(id);
        if (order.getStatus() != LeaseOrderStatus.ACTIVE) {
            throw new BusinessException("只有执行中的租赁单才能完成");
        }
        order.setStatus(LeaseOrderStatus.COMPLETED);
        order.setEndTime(LocalDateTime.now());
        Robot robot = robotService.findById(order.getRobotId());
        robotService.updateStatus(robot.getId(), RobotStatus.IDLE);
        return leaseOrderRepository.save(order);
    }

    public void checkSettlementLocked(Long leaseOrderId, String operation) {
        settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .ifPresent(settlement -> {
                    if (settlement.getStatus() == SettlementStatus.CONFIRMED) {
                        throw new BusinessException(403,
                                "结算已确认锁定，无法" + operation + "。如需修改，请先取消结算确认。");
                    }
                });
    }

    public boolean isSettlementConfirmed(Long leaseOrderId) {
        return settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .map(s -> s.getStatus() == SettlementStatus.CONFIRMED)
                .orElse(false);
    }

    @Transactional
    public void markSettled(Long id) {
        LeaseOrder order = findById(id);
        order.setStatus(LeaseOrderStatus.SETTLED);
        leaseOrderRepository.save(order);
    }
}
''',
    'RunningHourService.java': '''package com.robot.lease.service;

import com.robot.lease.dto.RunningHourDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.entity.MaintenanceRecord;
import com.robot.lease.entity.RunningHour;
import com.robot.lease.common.BusinessException;
import com.robot.lease.repository.MaintenanceRecordRepository;
import com.robot.lease.repository.RunningHourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningHourService {

    private final RunningHourRepository runningHourRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final LeaseOrderService leaseOrderService;

    public List<RunningHour> findByLeaseOrderId(Long leaseOrderId) {
        leaseOrderService.findById(leaseOrderId);
        return runningHourRepository.findByLeaseOrderIdOrderByReportDateAsc(leaseOrderId);
    }

    public RunningHour findById(Long id) {
        return runningHourRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "运行小时记录不存在: " + id));
    }

    @Transactional
    public RunningHour create(RunningHourDTO dto) {
        LeaseOrder order = leaseOrderService.findById(dto.getLeaseOrderId());
        leaseOrderService.checkSettlementLocked(dto.getLeaseOrderId(), "上报运行小时");

        runningHourRepository.findByLeaseOrderIdAndReportDate(dto.getLeaseOrderId(), dto.getReportDate())
                .ifPresent(existing -> {
                    throw new BusinessException("该日期的运行小时已上报，请使用修改功能");
                });

        RunningHour rh = new RunningHour();
        rh.setLeaseOrderId(dto.getLeaseOrderId());
        rh.setReportDate(dto.getReportDate());
        rh.setReportedHours(dto.getReportedHours());
        rh.setRemark(dto.getRemark());

        BigDecimal deduction = calculateDeductionForDate(dto.getLeaseOrderId(), dto.getReportDate());
        rh.setDeductionHours(deduction);
        rh.calculateBillableHours();

        RunningHour saved = runningHourRepository.save(rh);
        log.info("Created running hour: leaseOrderId={}, date={}, reported={}, deduction={}, billable={}",
                saved.getLeaseOrderId(), saved.getReportDate(), saved.getReportedHours(),
                saved.getDeductionHours(), saved.getBillableHours());
        return saved;
    }

    @Transactional
    public RunningHour update(Long id, RunningHourDTO dto) {
        RunningHour rh = findById(id);
        leaseOrderService.checkSettlementLocked(rh.getLeaseOrderId(), "修改运行小时");

        rh.setReportDate(dto.getReportDate());
        rh.setReportedHours(dto.getReportedHours());
        rh.setRemark(dto.getRemark());

        BigDecimal deduction = calculateDeductionForDate(rh.getLeaseOrderId(), dto.getReportDate());
        rh.setDeductionHours(deduction);
        rh.calculateBillableHours();

        return runningHourRepository.save(rh);
    }

    @Transactional
    public void delete(Long id) {
        RunningHour rh = findById(id);
        leaseOrderService.checkSettlementLocked(rh.getLeaseOrderId(), "删除运行小时");
        runningHourRepository.delete(rh);
    }

    public BigDecimal calculateDeductionForDate(Long leaseOrderId, LocalDate reportDate) {
        return maintenanceRecordRepository.sumDeductionHoursForDate(leaseOrderId, reportDate);
    }

    @Transactional
    public void recalculateDeductionsForLeaseOrder(Long leaseOrderId) {
        List<RunningHour> hours = runningHourRepository.findByLeaseOrderIdOrderByReportDateAsc(leaseOrderId);
        for (RunningHour rh : hours) {
            BigDecimal deduction = calculateDeductionForDate(leaseOrderId, rh.getReportDate());
            if (deduction.compareTo(rh.getDeductionHours()) != 0) {
                rh.setDeductionHours(deduction);
                rh.calculateBillableHours();
                runningHourRepository.save(rh);
                log.info("Recalculated running hour id={}: deduction {} -> {}, billable now {}",
                        rh.getId(), deduction, rh.getDeductionHours(), rh.getBillableHours());
            }
        }
    }

    public List<RunningHour> findByDateRange(Long leaseOrderId, LocalDate startDate, LocalDate endDate) {
        leaseOrderService.findById(leaseOrderId);
        return runningHourRepository.findByLeaseOrderIdAndDateRange(leaseOrderId, startDate, endDate);
    }
}
''',
    'MaintenanceRecordService.java': '''package com.robot.lease.service;

import com.robot.lease.dto.MaintenanceRecordDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.entity.MaintenanceRecord;
import com.robot.lease.entity.Robot;
import com.robot.lease.enums.MaintenanceStatus;
import com.robot.lease.common.BusinessException;
import com.robot.lease.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final LeaseOrderService leaseOrderService;
    private final RunningHourService runningHourService;
    private final RobotService robotService;

    public List<MaintenanceRecord> findByLeaseOrderId(Long leaseOrderId) {
        leaseOrderService.findById(leaseOrderId);
        return maintenanceRecordRepository.findByLeaseOrderIdOrderByStartTimeAsc(leaseOrderId);
    }

    public MaintenanceRecord findById(Long id) {
        return maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "维修记录不存在: " + id));
    }

    @Transactional
    public MaintenanceRecord create(MaintenanceRecordDTO dto) {
        LeaseOrder order = leaseOrderService.findById(dto.getLeaseOrderId());
        leaseOrderService.checkSettlementLocked(dto.getLeaseOrderId(), "登记维修记录");

        Robot robot = robotService.findById(order.getRobotId());

        MaintenanceRecord record = new MaintenanceRecord();
        record.setLeaseOrderId(dto.getLeaseOrderId());
        record.setRobotId(robot.getId());
        record.setStartTime(dto.getStartTime());
        record.setEndTime(dto.getEndTime());
        record.setDowntimeHours(dto.getDowntimeHours());
        record.setDescription(dto.getDescription());
        record.setMaintenanceCost(dto.getMaintenanceCost() != null ? dto.getMaintenanceCost() : java.math.BigDecimal.ZERO);
        record.setStatus(dto.getStatus() != null ? dto.getStatus() : MaintenanceStatus.PENDING);

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        log.info("Created maintenance record: id={}, leaseOrderId={}, downtimeHours={}",
                saved.getId(), saved.getLeaseOrderId(), saved.getDowntimeHours());

        runningHourService.recalculateDeductionsForLeaseOrder(dto.getLeaseOrderId());

        return saved;
    }

    @Transactional
    public MaintenanceRecord update(Long id, MaintenanceRecordDTO dto) {
        MaintenanceRecord record = findById(id);
        leaseOrderService.checkSettlementLocked(record.getLeaseOrderId(), "修改维修记录");

        record.setStartTime(dto.getStartTime());
        record.setEndTime(dto.getEndTime());
        record.setDowntimeHours(dto.getDowntimeHours());
        record.setDescription(dto.getDescription());
        record.setMaintenanceCost(dto.getMaintenanceCost() != null ? dto.getMaintenanceCost() : java.math.BigDecimal.ZERO);
        if (dto.getStatus() != null) {
            record.setStatus(dto.getStatus());
        }

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        log.info("Updated maintenance record: id={}, downtimeHours={}", saved.getId(), saved.getDowntimeHours());

        runningHourService.recalculateDeductionsForLeaseOrder(record.getLeaseOrderId());

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        MaintenanceRecord record = findById(id);
        leaseOrderService.checkSettlementLocked(record.getLeaseOrderId(), "删除维修记录");

        Long leaseOrderId = record.getLeaseOrderId();
        maintenanceRecordRepository.delete(record);
        log.info("Deleted maintenance record: id={}", id);

        runningHourService.recalculateDeductionsForLeaseOrder(leaseOrderId);
    }

    @Transactional
    public MaintenanceRecord updateStatus(Long id, MaintenanceStatus status) {
        MaintenanceRecord record = findById(id);
        leaseOrderService.checkSettlementLocked(record.getLeaseOrderId(), "修改维修状态");
        record.setStatus(status);
        return maintenanceRecordRepository.save(record);
    }
}
''',
    'SettlementService.java': '''package com.robot.lease.service;

import com.robot.lease.dto.SettlementDTO;
import com.robot.lease.entity.*;
import com.robot.lease.enums.LeaseOrderStatus;
import com.robot.lease.enums.SettlementStatus;
import com.robot.lease.common.BusinessException;
import com.robot.lease.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final LeaseOrderRepository leaseOrderRepository;
    private final RunningHourRepository runningHourRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final RobotRepository robotRepository;
    private final LeaseOrderService leaseOrderService;

    private static final DateTimeFormatter SETTLEMENT_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicInteger settlementCounter = new AtomicInteger(0);

    public Page<Settlement> findAll(Pageable pageable) {
        return settlementRepository.findAll(pageable);
    }

    public Settlement findById(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "结算单不存在: " + id));
    }

    public List<Settlement> findByLeaseOrderId(Long leaseOrderId) {
        leaseOrderRepository.findById(leaseOrderId)
                .orElseThrow(() -> new BusinessException(404, "租赁单不存在: " + leaseOrderId));
        return settlementRepository.findByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId);
    }

    private String generateSettlementNo() {
        String timestamp = LocalDateTime.now().format(SETTLEMENT_NO_FORMAT);
        int seq = settlementCounter.incrementAndGet() % 10000;
        return "ST" + timestamp + String.format("%04d", seq);
    }

    @Transactional
    public Settlement calculate(Long leaseOrderId) {
        LeaseOrder order = leaseOrderRepository.findById(leaseOrderId)
                .orElseThrow(() -> new BusinessException(404, "租赁单不存在: " + leaseOrderId));
        leaseOrderService.checkSettlementLocked(leaseOrderId, "重新计算结算");

        Robot robot = robotRepository.findById(order.getRobotId())
                .orElseThrow(() -> new BusinessException(404, "机器人不存在: " + order.getRobotId()));

        BigDecimal totalBillableHours = runningHourRepository.sumBillableHoursByLeaseOrderId(leaseOrderId);
        BigDecimal maintenanceTotal = maintenanceRecordRepository.sumMaintenanceCostByLeaseOrderId(leaseOrderId);
        BigDecimal baseRent = totalBillableHours.multiply(robot.getHourlyRate());
        BigDecimal totalAmount = baseRent.add(maintenanceTotal);

        Optional<Settlement> existingOpt = settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId);
        Settlement settlement;

        if (existingOpt.isPresent() && existingOpt.get().getStatus() == SettlementStatus.DRAFT) {
            settlement = existingOpt.get();
        } else {
            settlement = new Settlement();
            settlement.setSettlementNo(generateSettlementNo());
            settlement.setLeaseOrderId(leaseOrderId);
            settlement.setStatus(SettlementStatus.DRAFT);
        }

        settlement.setTotalBillableHours(totalBillableHours);
        settlement.setBaseRent(baseRent);
        settlement.setMaintenanceTotal(maintenanceTotal);
        settlement.setTotalAmount(totalAmount);

        Settlement saved = settlementRepository.save(settlement);
        log.info("Calculated settlement: id={}, leaseOrderId={}, totalHours={}, baseRent={}, maintTotal={}, total={}",
                saved.getId(), leaseOrderId, totalBillableHours, baseRent, maintenanceTotal, totalAmount);
        return saved;
    }

    @Transactional
    public Settlement review(Long leaseOrderId) {
        Settlement settlement = settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .orElseThrow(() -> new BusinessException("请先计算结算"));

        if (settlement.getStatus() != SettlementStatus.DRAFT) {
            throw new BusinessException("只有待复核状态的结算单才能复核");
        }

        settlement.setStatus(SettlementStatus.REVIEWED);
        settlement.setReviewTime(LocalDateTime.now());
        Settlement saved = settlementRepository.save(settlement);
        log.info("Reviewed settlement: id={}, status=REVIEWED", saved.getId());
        return saved;
    }

    @Transactional
    public Settlement confirm(Long leaseOrderId) {
        Settlement settlement = settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .orElseThrow(() -> new BusinessException("请先计算结算"));

        if (settlement.getStatus() != SettlementStatus.REVIEWED) {
            throw new BusinessException("只有待确认状态的结算单才能确认");
        }

        settlement.setStatus(SettlementStatus.CONFIRMED);
        settlement.setConfirmTime(LocalDateTime.now());
        Settlement saved = settlementRepository.save(settlement);

        LeaseOrder order = leaseOrderRepository.findById(leaseOrderId).orElseThrow();
        if (order.getStatus() != LeaseOrderStatus.SETTLED) {
            order.setStatus(LeaseOrderStatus.SETTLED);
            leaseOrderRepository.save(order);
        }

        log.info("Confirmed settlement (LOCKED): id={}, status=CONFIRMED, leaseOrder now SETTLED", saved.getId());
        return saved;
    }

    @Transactional
    public Settlement cancelConfirm(Long leaseOrderId) {
        Settlement settlement = settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .orElseThrow(() -> new BusinessException("结算单不存在"));

        if (settlement.getStatus() != SettlementStatus.CONFIRMED) {
            throw new BusinessException("只有已确认状态的结算单才能取消确认");
        }

        settlement.setStatus(SettlementStatus.REVIEWED);
        settlement.setConfirmTime(null);
        Settlement saved = settlementRepository.save(settlement);

        LeaseOrder order = leaseOrderRepository.findById(leaseOrderId).orElseThrow();
        if (order.getStatus() == LeaseOrderStatus.SETTLED) {
            order.setStatus(LeaseOrderStatus.COMPLETED);
            leaseOrderRepository.save(order);
        }

        log.info("Cancelled settlement confirm: id={}, status back to REVIEWED", saved.getId());
        return saved;
    }
}
'''
}

print('=== Creating Services ===')
for name, content in services.items():
    write_file(os.path.join(BASE_DIR, 'service', name), content)

# ============ Controllers ============
controllers = {
    'RobotController.java': '''package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.RobotDTO;
import com.robot.lease.entity.Robot;
import com.robot.lease.service.RobotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/robots")
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;

    @GetMapping
    public Result<Page<Robot>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Result.success(robotService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public Result<Robot> getById(@PathVariable Long id) {
        return Result.success(robotService.findById(id));
    }

    @PostMapping
    public Result<Robot> create(@Valid @RequestBody RobotDTO dto) {
        return Result.success(robotService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Robot> update(@PathVariable Long id, @Valid @RequestBody RobotDTO dto) {
        return Result.success(robotService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        robotService.delete(id);
        return Result.success();
    }
}
''',
    'LeaseOrderController.java': '''package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.LeaseOrderDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.service.LeaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lease-orders")
@RequiredArgsConstructor
public class LeaseOrderController {

    private final LeaseOrderService leaseOrderService;

    @GetMapping
    public Result<Page<LeaseOrder>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Result.success(leaseOrderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public Result<LeaseOrder> getById(@PathVariable Long id) {
        return Result.success(leaseOrderService.findById(id));
    }

    @PostMapping
    public Result<LeaseOrder> create(@Valid @RequestBody LeaseOrderDTO dto) {
        return Result.success(leaseOrderService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<LeaseOrder> update(@PathVariable Long id, @Valid @RequestBody LeaseOrderDTO dto) {
        return Result.success(leaseOrderService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        leaseOrderService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/activate")
    public Result<LeaseOrder> activate(@PathVariable Long id) {
        return Result.success(leaseOrderService.activate(id));
    }

    @PostMapping("/{id}/complete")
    public Result<LeaseOrder> complete(@PathVariable Long id) {
        return Result.success(leaseOrderService.complete(id));
    }

    @GetMapping("/{id}/settlement-status")
    public Result<Boolean> getSettlementStatus(@PathVariable Long id) {
        return Result.success(leaseOrderService.isSettlementConfirmed(id));
    }
}
''',
    'RunningHourController.java': '''package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.RunningHourDTO;
import com.robot.lease.entity.RunningHour;
import com.robot.lease.service.RunningHourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lease-orders/{leaseOrderId}/running-hours")
@RequiredArgsConstructor
public class RunningHourController {

    private final RunningHourService runningHourService;

    @GetMapping
    public Result<List<RunningHour>> list(@PathVariable Long leaseOrderId) {
        return Result.success(runningHourService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/all")
    public Result<List<RunningHour>> listAll(@PathVariable Long leaseOrderId) {
        return Result.success(runningHourService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/{id}")
    public Result<RunningHour> getById(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        return Result.success(runningHourService.findById(id));
    }

    @GetMapping("/range")
    public Result<List<RunningHour>> listByDateRange(
            @PathVariable Long leaseOrderId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(runningHourService.findByDateRange(leaseOrderId, startDate, endDate));
    }

    @PostMapping
    public Result<RunningHour> create(@PathVariable Long leaseOrderId, @Valid @RequestBody RunningHourDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(runningHourService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<RunningHour> update(@PathVariable Long leaseOrderId, @PathVariable Long id,
                                      @Valid @RequestBody RunningHourDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(runningHourService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        runningHourService.delete(id);
        return Result.success();
    }

    @PostMapping("/recalculate")
    public Result<Void> recalculate(@PathVariable Long leaseOrderId) {
        runningHourService.recalculateDeductionsForLeaseOrder(leaseOrderId);
        return Result.success();
    }
}
''',
    'MaintenanceRecordController.java': '''package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.MaintenanceRecordDTO;
import com.robot.lease.entity.MaintenanceRecord;
import com.robot.lease.enums.MaintenanceStatus;
import com.robot.lease.service.MaintenanceRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lease-orders/{leaseOrderId}/maintenance")
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    @GetMapping
    public Result<List<MaintenanceRecord>> list(@PathVariable Long leaseOrderId) {
        return Result.success(maintenanceRecordService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/all")
    public Result<List<MaintenanceRecord>> listAll(@PathVariable Long leaseOrderId) {
        return Result.success(maintenanceRecordService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/{id}")
    public Result<MaintenanceRecord> getById(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        return Result.success(maintenanceRecordService.findById(id));
    }

    @PostMapping
    public Result<MaintenanceRecord> create(@PathVariable Long leaseOrderId,
                                            @Valid @RequestBody MaintenanceRecordDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(maintenanceRecordService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<MaintenanceRecord> update(@PathVariable Long leaseOrderId, @PathVariable Long id,
                                            @Valid @RequestBody MaintenanceRecordDTO dto) {
        dto.setLeaseOrderId(leaseOrderId);
        return Result.success(maintenanceRecordService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long leaseOrderId, @PathVariable Long id) {
        maintenanceRecordService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/status")
    public Result<MaintenanceRecord> updateStatus(@PathVariable Long leaseOrderId, @PathVariable Long id,
                                                  @RequestParam MaintenanceStatus status) {
        return Result.success(maintenanceRecordService.updateStatus(id, status));
    }
}
''',
    'SettlementController.java': '''package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.entity.Settlement;
import com.robot.lease.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lease-orders/{leaseOrderId}/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public Result<List<Settlement>> list(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.findByLeaseOrderId(leaseOrderId));
    }

    @GetMapping("/latest")
    public Result<Settlement> getLatest(@PathVariable Long leaseOrderId) {
        List<Settlement> list = settlementService.findByLeaseOrderId(leaseOrderId);
        return Result.success(list.isEmpty() ? null : list.get(0));
    }

    @PostMapping("/calculate")
    public Result<Settlement> calculate(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.calculate(leaseOrderId));
    }

    @PostMapping("/review")
    public Result<Settlement> review(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.review(leaseOrderId));
    }

    @PostMapping("/confirm")
    public Result<Settlement> confirm(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.confirm(leaseOrderId));
    }

    @PostMapping("/cancel-confirm")
    public Result<Settlement> cancelConfirm(@PathVariable Long leaseOrderId) {
        return Result.success(settlementService.cancelConfirm(leaseOrderId));
    }
}
''',
    'DashboardController.java': '''package com.robot.lease.controller;

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
'''
}

print('=== Creating Controllers ===')
for name, content in controllers.items():
    write_file(os.path.join(BASE_DIR, 'controller', name), content)

# ============ Main Application ============
main_app = {
    'RobotLeaseApplication.java': '''package com.robot.lease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class RobotLeaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotLeaseApplication.class, args);
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "robot-lease-service");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
'''
}

print('=== Creating Main Application ===')
for name, content in main_app.items():
    write_file(os.path.join(BASE_DIR, name), content)

# ============ Configs ============
configs = {
    'CorsConfig.java': '''package com.robot.lease.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
'''
}

print('=== Creating Configs ===')
for name, content in configs.items():
    write_file(os.path.join(BASE_DIR, 'config', name), content)

# ============ Resources: application.yml ============
app_yml = '''server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: robot-lease-service

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/robot_lease}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10
      idle-timeout: 60000
      max-lifetime: 1800000
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: Asia/Shanghai
    open-in-view: false

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false

logging:
  level:
    root: INFO
    com.robot.lease: DEBUG
    org.hibernate.SQL: INFO
'''

print('=== Creating application.yml ===')
write_file(os.path.join(RESOURCE_DIR, 'application.yml'), app_yml)

# ============ pom.xml ============
pom_xml = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.robot</groupId>
    <artifactId>robot-lease</artifactId>
    <version>1.0.0</version>
    <name>robot-lease</name>
    <description>工业机器人租赁维修结算系统</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
'''

print('=== Creating pom.xml ===')
write_file(os.path.join(ROOT_DIR, 'pom.xml'), pom_xml)

print('\n=== ALL BACKEND FILES GENERATED ===')

