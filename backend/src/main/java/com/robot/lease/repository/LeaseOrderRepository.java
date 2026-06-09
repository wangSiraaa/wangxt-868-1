package com.robot.lease.repository;

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
