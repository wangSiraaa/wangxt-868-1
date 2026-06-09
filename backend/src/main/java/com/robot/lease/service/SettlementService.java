package com.robot.lease.service;

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
