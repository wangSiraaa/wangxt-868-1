package com.robot.lease.service;

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

        java.math.BigDecimal downtime = dto.getDowntimeHours();
        if (downtime == null || downtime.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (dto.getStartTime() != null && dto.getEndTime() != null) {
                long minutes = java.time.Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
                downtime = java.math.BigDecimal.valueOf(minutes)
                        .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                if (downtime.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    downtime = java.math.BigDecimal.ZERO;
                }
            } else {
                downtime = java.math.BigDecimal.ZERO;
            }
        }

        MaintenanceRecord record = new MaintenanceRecord();
        record.setLeaseOrderId(dto.getLeaseOrderId());
        record.setRobotId(robot.getId());
        record.setStartTime(dto.getStartTime());
        record.setEndTime(dto.getEndTime());
        record.setDowntimeHours(downtime);
        record.setDescription(dto.getDescription() != null ? dto.getDescription() : "维修登记");
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

        java.math.BigDecimal downtime = dto.getDowntimeHours();
        if (dto.getStartTime() != null) record.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) record.setEndTime(dto.getEndTime());
        if (downtime == null || downtime.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (record.getStartTime() != null && record.getEndTime() != null) {
                long minutes = java.time.Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
                downtime = java.math.BigDecimal.valueOf(minutes)
                        .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                if (downtime.compareTo(java.math.BigDecimal.ZERO) < 0) downtime = java.math.BigDecimal.ZERO;
            } else {
                downtime = record.getDowntimeHours();
            }
        }
        record.setDowntimeHours(downtime);
        if (dto.getDescription() != null) record.setDescription(dto.getDescription());
        record.setMaintenanceCost(dto.getMaintenanceCost() != null ? dto.getMaintenanceCost() : record.getMaintenanceCost());
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
