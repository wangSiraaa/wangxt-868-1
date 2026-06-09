package com.robot.lease.controller;

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
