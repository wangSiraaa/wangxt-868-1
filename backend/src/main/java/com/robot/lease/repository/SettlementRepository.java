package com.robot.lease.repository;

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
