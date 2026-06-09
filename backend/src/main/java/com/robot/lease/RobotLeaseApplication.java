package com.robot.lease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class RobotLeaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotLeaseApplication.class, args);
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "robot-lease-service");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
