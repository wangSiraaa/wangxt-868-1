package com.robot.lease.service;

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
