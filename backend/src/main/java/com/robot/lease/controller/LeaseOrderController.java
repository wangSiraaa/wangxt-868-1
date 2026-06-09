package com.robot.lease.controller;

import com.robot.lease.common.Result;
import com.robot.lease.dto.BatchImportResultDTO;
import com.robot.lease.dto.LeaseOrderDTO;
import com.robot.lease.dto.TimelineEventDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.service.LeaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/lease-orders")
@RequiredArgsConstructor
public class LeaseOrderController {

    private final LeaseOrderService leaseOrderService;

    @GetMapping
    public Result<Page<LeaseOrder>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return Result.success(leaseOrderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public Result<LeaseOrder> getById(@PathVariable Long id) {
        return Result.success(leaseOrderService.findById(id));
    }

    @PostMapping
    public Result<LeaseOrder> create(@Valid @RequestBody LeaseOrderDTO dto) {
        return Result.success(leaseOrderService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<LeaseOrder> update(@PathVariable Long id, @Valid @RequestBody LeaseOrderDTO dto) {
        return Result.success(leaseOrderService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        leaseOrderService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/activate")
    public Result<LeaseOrder> activate(@PathVariable Long id) {
        return Result.success(leaseOrderService.activate(id));
    }

    @PostMapping("/{id}/complete")
    public Result<LeaseOrder> complete(@PathVariable Long id) {
        return Result.success(leaseOrderService.complete(id));
    }

    @GetMapping("/{id}/settlement-status")
    public Result<Boolean> getSettlementStatus(@PathVariable Long id) {
        return Result.success(leaseOrderService.isSettlementConfirmed(id));
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] bytes = leaseOrderService.downloadImportTemplate();
        String filename = URLEncoder.encode("租赁单导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<BatchImportResultDTO> batchImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("请上传Excel文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.toLowerCase().endsWith(".xlsx")
                && !originalName.toLowerCase().endsWith(".xls"))) {
            return Result.error("仅支持 .xlsx 或 .xls 格式的Excel文件");
        }
        BatchImportResultDTO result = leaseOrderService.batchImportLeaseOrders(file.getBytes());
        return Result.success(result);
    }

    @GetMapping("/{id}/timeline")
    public Result<List<TimelineEventDTO>> getTimeline(@PathVariable Long id) {
        return Result.success(leaseOrderService.getTimeline(id));
    }
}
