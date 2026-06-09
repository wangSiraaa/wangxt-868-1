package com.robot.lease.service;

import com.robot.lease.dto.LeaseOrderDTO;
import com.robot.lease.entity.LeaseOrder;
import com.robot.lease.entity.Robot;
import com.robot.lease.entity.Settlement;
import com.robot.lease.enums.LeaseOrderStatus;
import com.robot.lease.enums.RobotStatus;
import com.robot.lease.enums.SettlementStatus;
import com.robot.lease.common.BusinessException;
import com.robot.lease.repository.LeaseOrderRepository;
import com.robot.lease.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseOrderService {

    private final LeaseOrderRepository leaseOrderRepository;
    private final RobotService robotService;
    private final SettlementRepository settlementRepository;
    private final RobotRepository robotRepository;
    private final RunningHourRepository runningHourRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicInteger orderCounter = new AtomicInteger(0);

    public Page<LeaseOrder> findAll(Pageable pageable) {
        return leaseOrderRepository.findAll(pageable);
    }

    public LeaseOrder findById(Long id) {
        return leaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "租赁单不存在: " + id));
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FORMAT);
        int seq = orderCounter.incrementAndGet() % 10000;
        return "LO" + timestamp + String.format("%04d", seq);
    }

    @Transactional
    public LeaseOrder create(LeaseOrderDTO dto) {
        Robot robot = robotService.findById(dto.getRobotId());

        LeaseOrder order = new LeaseOrder();
        order.setOrderNo(generateOrderNo());
        order.setLesseeFactory(dto.getLesseeFactory());
        order.setContactPerson(dto.getContactPerson());
        order.setContactPhone(dto.getContactPhone());
        order.setRobotId(dto.getRobotId());
        order.setStartTime(dto.getStartTime());
        order.setEndTime(dto.getEndTime());
        order.setRemark(dto.getRemark());
        order.setStatus(LeaseOrderStatus.DRAFT);
        return leaseOrderRepository.save(order);
    }

    @Transactional
    public LeaseOrder update(Long id, LeaseOrderDTO dto) {
        LeaseOrder order = findById(id);
        checkSettlementLocked(id, "修改租赁单");

        if (!order.getRobotId().equals(dto.getRobotId())) {
            robotService.findById(dto.getRobotId());
        }
        order.setLesseeFactory(dto.getLesseeFactory());
        order.setContactPerson(dto.getContactPerson());
        order.setContactPhone(dto.getContactPhone());
        order.setRobotId(dto.getRobotId());
        order.setStartTime(dto.getStartTime());
        order.setEndTime(dto.getEndTime());
        order.setRemark(dto.getRemark());
        return leaseOrderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        LeaseOrder order = findById(id);
        if (order.getStatus() != LeaseOrderStatus.DRAFT) {
            throw new BusinessException("只能删除草稿状态的租赁单");
        }
        checkSettlementLocked(id, "删除租赁单");
        leaseOrderRepository.delete(order);
    }

    @Transactional
    public LeaseOrder activate(Long id) {
        LeaseOrder order = findById(id);
        if (order.getStatus() != LeaseOrderStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的租赁单才能激活");
        }
        Robot robot = robotService.findById(order.getRobotId());
        if (robot.getStatus() == RobotStatus.RENTED) {
            throw new BusinessException("该设备已被其他租赁单占用");
        }
        order.setStatus(LeaseOrderStatus.ACTIVE);
        robotService.updateStatus(robot.getId(), RobotStatus.RENTED);
        return leaseOrderRepository.save(order);
    }

    @Transactional
    public LeaseOrder complete(Long id) {
        LeaseOrder order = findById(id);
        if (order.getStatus() != LeaseOrderStatus.ACTIVE) {
            throw new BusinessException("只有执行中的租赁单才能完成");
        }
        order.setStatus(LeaseOrderStatus.COMPLETED);
        order.setEndTime(LocalDateTime.now());
        Robot robot = robotService.findById(order.getRobotId());
        robotService.updateStatus(robot.getId(), RobotStatus.IDLE);
        return leaseOrderRepository.save(order);
    }

    public void checkSettlementLocked(Long leaseOrderId, String operation) {
        settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .ifPresent(settlement -> {
                    if (settlement.getStatus() == SettlementStatus.CONFIRMED) {
                        throw new BusinessException(403,
                                "结算已确认锁定，无法" + operation + "。如需修改，请先取消结算确认。");
                    }
                });
    }

    public boolean isSettlementConfirmed(Long leaseOrderId) {
        return settlementRepository.findFirstByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId)
                .map(s -> s.getStatus() == SettlementStatus.CONFIRMED)
                .orElse(false);
    }

    @Transactional
    public void markSettled(Long id) {
        LeaseOrder order = findById(id);
        order.setStatus(LeaseOrderStatus.SETTLED);
        leaseOrderRepository.save(order);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] downloadImportTemplate() {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("租赁单导入模板");

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                "订单号(可留空自动生成)", "承租工厂*", "联系人", "联系电话",
                "设备编码*", "租赁开始时间* (yyyy-MM-dd HH:mm)", "租赁结束时间 (yyyy-MM-dd HH:mm)", "备注"
            };
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            org.apache.poi.ss.usermodel.Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("LO-示例-001");
            exampleRow.createCell(1).setCellValue("上海汽配制造有限公司");
            exampleRow.createCell(2).setCellValue("张三");
            exampleRow.createCell(3).setCellValue("13800138000");
            exampleRow.createCell(4).setCellValue("RB-A001");
            exampleRow.createCell(5).setCellValue("2025-01-15 08:00");
            exampleRow.createCell(6).setCellValue("2025-06-15 18:00");
            exampleRow.createCell(7).setCellValue("6个月租期，含基础维护");

            int[] widths = {6000, 8000, 4000, 4000, 4000, 6500, 6500, 8000};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new BusinessException("生成模板失败: " + e.getMessage());
        }
    }

    @Transactional
    public com.robot.lease.dto.BatchImportResultDTO batchImportLeaseOrders(byte[] fileBytes) {
        com.robot.lease.dto.BatchImportResultDTO result = new com.robot.lease.dto.BatchImportResultDTO();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        java.util.concurrent.atomic.AtomicInteger rowCounter = new AtomicInteger(0);

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(fileBytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getPhysicalNumberOfRows();
            result.setTotalCount(Math.max(0, totalRows - 1));

            for (int rowIndex = 1; rowIndex < totalRows; rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row)) continue;

                int excelRowNum = rowIndex + 1;
                try {
                    String orderNo = getCellStringValue(row.getCell(0));
                    String lesseeFactory = getCellStringValue(row.getCell(1));
                    String contactPerson = getCellStringValue(row.getCell(2));
                    String contactPhone = getCellStringValue(row.getCell(3));
                    String robotCode = getCellStringValue(row.getCell(4));
                    String startTimeStr = getCellStringValue(row.getCell(5));
                    String endTimeStr = getCellStringValue(row.getCell(6));
                    String remark = getCellStringValue(row.getCell(7));

                    if (org.springframework.util.StringUtils.hasText(lesseeFactory)) {
                        lesseeFactory = lesseeFactory.trim();
                    }
                    if (!org.springframework.util.StringUtils.hasText(lesseeFactory)) {
                        addError(result, excelRowNum, "承租工厂", "承租工厂不能为空", lesseeFactory);
                        continue;
                    }

                    if (org.springframework.util.StringUtils.hasText(robotCode)) {
                        robotCode = robotCode.trim();
                    }
                    if (!org.springframework.util.StringUtils.hasText(robotCode)) {
                        addError(result, excelRowNum, "设备编码", "设备编码不能为空", robotCode);
                        continue;
                    }
                    Robot robot = robotRepository.findByCode(robotCode).orElse(null);
                    if (robot == null) {
                        addError(result, excelRowNum, "设备编码", "设备编码不存在: " + robotCode, robotCode);
                        continue;
                    }

                    if (!org.springframework.util.StringUtils.hasText(startTimeStr)) {
                        addError(result, excelRowNum, "租赁开始时间", "租赁开始时间不能为空", startTimeStr);
                        continue;
                    }
                    LocalDateTime startTime;
                    try {
                        startTime = LocalDateTime.parse(startTimeStr.trim(), dtf);
                    } catch (Exception e) {
                        addError(result, excelRowNum, "租赁开始时间", "时间格式错误，请使用 yyyy-MM-dd HH:mm，例如 2025-01-15 08:00", startTimeStr);
                        continue;
                    }

                    LocalDateTime endTime = null;
                    if (org.springframework.util.StringUtils.hasText(endTimeStr)) {
                        try {
                            endTime = LocalDateTime.parse(endTimeStr.trim(), dtf);
                            if (endTime.isBefore(startTime)) {
                                addError(result, excelRowNum, "租赁结束时间", "结束时间不能早于开始时间", endTimeStr);
                                continue;
                            }
                        } catch (Exception e) {
                            addError(result, excelRowNum, "租赁结束时间", "时间格式错误，请使用 yyyy-MM-dd HH:mm", endTimeStr);
                            continue;
                        }
                    }

                    if (org.springframework.util.StringUtils.hasText(orderNo)) {
                        orderNo = orderNo.trim();
                        if (leaseOrderRepository.existsByOrderNo(orderNo)) {
                            addError(result, excelRowNum, "订单号", "订单号已存在: " + orderNo, orderNo);
                            continue;
                        }
                    } else {
                        orderNo = generateOrderNo();
                    }

                    LeaseOrder order = new LeaseOrder();
                    order.setOrderNo(orderNo);
                    order.setLesseeFactory(lesseeFactory);
                    order.setContactPerson(contactPerson != null ? contactPerson.trim() : null);
                    order.setContactPhone(contactPhone != null ? contactPhone.trim() : null);
                    order.setRobotId(robot.getId());
                    order.setStartTime(startTime);
                    order.setEndTime(endTime);
                    order.setRemark(remark != null ? remark.trim() : null);
                    order.setStatus(LeaseOrderStatus.DRAFT);

                    LeaseOrder saved = leaseOrderRepository.save(order);
                    result.getSuccessOrderNos().add(saved.getOrderNo());
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    rowCounter.incrementAndGet();
                } catch (Exception e) {
                    addError(result, excelRowNum, "系统", "处理异常: " + e.getMessage(), "");
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败: " + e.getMessage());
        }

        result.setFailCount(result.getErrors().size());
        return result;
    }

    private boolean isRowEmpty(org.apache.poi.ss.usermodel.Row row) {
        for (int i = 0; i < 8; i++) {
            org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
            if (cell != null) {
                String val = getCellStringValue(cell);
                if (org.springframework.util.StringUtils.hasText(val)) return false;
            }
        }
        return true;
    }

    private String getCellStringValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    java.time.LocalDateTime dt = cell.getLocalDateTimeCellValue();
                    if (dt != null) {
                        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        return dt.format(dtf);
                    }
                    return null;
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private void addError(com.robot.lease.dto.BatchImportResultDTO result, int rowNum,
                          String field, String message, String value) {
        result.getErrors().add(new com.robot.lease.dto.BatchImportResultDTO.ImportError(rowNum, field, message, value));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.robot.lease.dto.TimelineEventDTO> getTimeline(Long leaseOrderId) {
        LeaseOrder order = findById(leaseOrderId);
        java.util.List<com.robot.lease.dto.TimelineEventDTO> events = new java.util.ArrayList<>();

        com.robot.lease.dto.TimelineEventDTO createdEvent = new com.robot.lease.dto.TimelineEventDTO();
        createdEvent.setId(order.getId());
        createdEvent.setType("ORDER");
        createdEvent.setTypeName("订单创建");
        createdEvent.setTitle("租赁单创建");
        createdEvent.setDescription("订单号: " + order.getOrderNo() + "，承租工厂: " + order.getLesseeFactory());
        createdEvent.setEventTime(order.getCreatedAt());
        createdEvent.setCreatedAt(order.getCreatedAt());
        createdEvent.setColor("#1677ff");
        events.add(createdEvent);

        if (order.getStatus() != LeaseOrderStatus.DRAFT && order.getStartTime() != null) {
            com.robot.lease.dto.TimelineEventDTO startEvent = new com.robot.lease.dto.TimelineEventDTO();
            startEvent.setId(order.getId());
            startEvent.setType("ORDER_ACTIVE");
            startEvent.setTypeName("订单激活");
            startEvent.setTitle("租赁开始执行");
            startEvent.setDescription("租期开始: " + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(order.getStartTime()));
            startEvent.setEventTime(order.getStartTime());
            startEvent.setColor("#52c41a");
            events.add(startEvent);
        }

        if (order.getEndTime() != null && (order.getStatus() == LeaseOrderStatus.COMPLETED || order.getStatus() == LeaseOrderStatus.SETTLED)) {
            com.robot.lease.dto.TimelineEventDTO endEvent = new com.robot.lease.dto.TimelineEventDTO();
            endEvent.setId(order.getId());
            endEvent.setType("ORDER_COMPLETE");
            endEvent.setTypeName("订单完成");
            endEvent.setTitle("租赁订单完成");
            endEvent.setDescription("租期结束: " + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(order.getEndTime()));
            endEvent.setEventTime(order.getEndTime());
            endEvent.setColor("#faad14");
            events.add(endEvent);
        }

        java.util.List<RunningHour> rhList = runningHourRepository.findByLeaseOrderIdOrderByReportDateAsc(leaseOrderId);
        for (RunningHour rh : rhList) {
            com.robot.lease.dto.TimelineEventDTO e = new com.robot.lease.dto.TimelineEventDTO();
            e.setId(rh.getId());
            e.setType("RUNNING_HOUR");
            e.setTypeName("运行小时上报");
            e.setTitle(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(rh.getReportDate()) + " 运行上报");
            e.setDescription("上报: " + rh.getReportedHours() + "h，扣减: " + rh.getDeductionHours() + "h，计费: " + rh.getBillableHours() + "h");
            e.setEventTime(rh.getReportDate().atTime(9, 0));
            e.setCreatedAt(rh.getCreatedAt());
            e.setColor("#13c2c2");
            e.setExtra(rh);
            events.add(e);
        }

        java.util.List<MaintenanceRecord> mtList = maintenanceRecordRepository.findByLeaseOrderIdOrderByStartTimeAsc(leaseOrderId);
        for (MaintenanceRecord mt : mtList) {
            com.robot.lease.dto.TimelineEventDTO e = new com.robot.lease.dto.TimelineEventDTO();
            e.setId(mt.getId());
            e.setType("MAINTENANCE");
            e.setTypeName("维修登记");
            String statusText = switch (mt.getStatus()) {
                case PENDING -> "待处理";
                case IN_PROGRESS -> "维修中";
                case COMPLETED -> "已完成";
            };
            e.setTitle("维修 - " + statusText);
            String desc = mt.getDescription();
            if (desc != null && desc.length() > 60) desc = desc.substring(0, 60) + "...";
            e.setDescription("停机 " + mt.getDowntimeHours() + "h，费用 ¥" + mt.getMaintenanceCost() + "｜" + (desc != null ? desc : ""));
            e.setEventTime(mt.getStartTime());
            e.setCreatedAt(mt.getCreatedAt());
            e.setColor(mt.getStatus() == com.robot.lease.enums.MaintenanceStatus.COMPLETED ? "#52c41a" : "#fa8c16");
            e.setExtra(mt);
            events.add(e);
        }

        java.util.List<Settlement> sList = settlementRepository.findByLeaseOrderIdOrderByCreatedAtDesc(leaseOrderId);
        for (Settlement s : sList) {
            com.robot.lease.dto.TimelineEventDTO e = new com.robot.lease.dto.TimelineEventDTO();
            e.setId(s.getId());
            String statusText = switch (s.getStatus()) {
                case DRAFT -> "待复核";
                case REVIEWED -> "待确认";
                case CONFIRMED -> "已结算";
            };
            e.setType("SETTLEMENT_" + s.getStatus());
            e.setTypeName("结算");
            e.setTitle("结算单 - " + statusText);
            e.setDescription("单号 " + s.getSettlementNo() + "，总计费 " + s.getTotalBillableHours() + "h，总金额 ¥" + s.getTotalAmount());
            LocalDateTime t = s.getConfirmTime() != null ? s.getConfirmTime() :
                    (s.getReviewTime() != null ? s.getReviewTime() : s.getCreatedAt());
            e.setEventTime(t);
            e.setCreatedAt(s.getCreatedAt());
            e.setColor(s.getStatus() == com.robot.lease.enums.SettlementStatus.CONFIRMED ? "#722ed1" : "#8c8c8c");
            e.setExtra(s);
            events.add(e);
        }

        events.sort((a, b) -> {
            LocalDateTime ta = a.getEventTime() != null ? a.getEventTime() : a.getCreatedAt();
            LocalDateTime tb = b.getEventTime() != null ? b.getEventTime() : b.getCreatedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });

        return events;
    }
}
