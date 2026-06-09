package com.robot.lease.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimelineEventDTO {
    private Long id;
    private String type;
    private String typeName;
    private String title;
    private String description;
    private LocalDateTime eventTime;
    private LocalDateTime createdAt;
    private String color;
    private Object extra;
}
