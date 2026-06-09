package com.robot.lease.service;

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
