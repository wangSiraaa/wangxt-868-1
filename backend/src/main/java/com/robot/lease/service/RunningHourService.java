package com.robot.lease.service;

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
