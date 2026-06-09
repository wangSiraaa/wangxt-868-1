package com.robot.lease.repository;

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
