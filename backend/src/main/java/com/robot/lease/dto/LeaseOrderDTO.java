package com.robot.lease.dto;

import com.robot.lease.enums.LeaseOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LeaseOrderDTO {
    private Long id;
    private String orderNo;

    @NotBlank(message = "承租工厂不能为空")
    private String lesseeFactory;

    private String contactPerson;
    private String contactPhone;

    @NotNull(message = "机器人ID不能为空")
    private Long robotId;

    @NotNull(message = "租赁开始时间不能为空")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String remark;
    private LeaseOrderStatus status = LeaseOrderStatus.DRAFT;
}
