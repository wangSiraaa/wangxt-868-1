package com.robot.lease.dto;

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
