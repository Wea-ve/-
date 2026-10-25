package org.example.nongye.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.nongye.common.ApiException;
import org.example.nongye.common.ApiResult;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.example.nongye.service.TrustScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者端公开 REST 接口（无需登录）：扫码溯源详情 + 投诉
 */
@RestController
@RequestMapping("/api")
public class ApiTraceController {

    private static final Logger log = LoggerFactory.getLogger(ApiTraceController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private ScanLogMapper scanLogMapper;
    @Autowired
    private TrustScoreService trustScoreService;

    /** 扫码溯源详情（公开）：记录扫码日志，供扫码统计 */
    @GetMapping("/trace/{traceCode}")
    public ApiResult<Map<String, Object>> traceDetail(@PathVariable String traceCode, HttpServletRequest request) {
        CropBatch batch = batchMapper.selectByTraceCode(traceCode);
        if (batch == null) {
            throw ApiException.badRequest("溯源码无效或不存在");
        }
        // 记录扫码日志
        try {
            ScanLog scanLog = new ScanLog();
            scanLog.setBatchId(batch.getId());
            scanLog.setTraceCode(traceCode);
            scanLog.setIp(getClientIp(request));
            scanLog.setUserAgent(request.getHeader("User-Agent"));
            scanLogMapper.insert(scanLog);
        } catch (Exception e) {
            log.warn("扫码日志记录失败", e);
        }

        List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batch.getId());
        List<TraceRecord> records = traceRecordMapper.selectByBatchId(batch.getId());
        List<DetectionReport> reports = reportMapper.selectByBatchId(batch.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("batch", batch);
        data.put("logs", logs);
        data.put("records", records);
        data.put("reports", reports);
        // 可信徽章：流转节点数
        data.put("nodeCount", traceRecordMapper.countByBatchId(batch.getId()));
        // AI 综合可信度评分：扫码先看红黄绿评分卡，再看明细
        data.put("trustScore", trustScoreService.score(batch, logs, records, reports));
        return ApiResult.ok(data);
    }

    /** 消费者提交投诉（公开）：投诉人和内容必填 */
    @PostMapping("/complaint")
    public ApiResult<Map<String, Object>> submitComplaint(@RequestBody Complaint complaint) {
        if (isBlank(complaint.getComplainant())) {
            throw ApiException.badRequest("投诉人不能为空");
        }
        if (isBlank(complaint.getContent())) {
            throw ApiException.badRequest("投诉内容不能为空");
        }
        if (complaint.getBatchId() != null && batchMapper.selectById(complaint.getBatchId()) == null) {
            throw ApiException.badRequest("关联批次不存在");
        }
        // 生成查询码：消费者凭此查询进度/确认办结，避免用自增 ID 枚举他人投诉
        complaint.setQueryCode(generateQueryCode());
        complaintMapper.insert(complaint);
        log.info("收到投诉：{} 批次ID：{} 查询码：{}",
                complaint.getComplainant(), complaint.getBatchId(), complaint.getQueryCode());
        Map<String, Object> data = new HashMap<>();
        data.put("id", complaint.getId());
        data.put("queryCode", complaint.getQueryCode());
        if (complaint.getBatchId() != null) {
            CropBatch batch = batchMapper.selectById(complaint.getBatchId());
            if (batch != null && batch.getTraceCode() != null) {
                data.put("traceCode", batch.getTraceCode());
            }
        }
        return ApiResult.ok(data);
    }

    /** 消费者按查询码或手机号查询投诉进度（公开，手机号做脱敏返回） */
    @GetMapping("/complaint/query")
    public ApiResult<List<Complaint>> complaintQuery(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResult.ok(List.of());
        }
        List<Complaint> list = complaintMapper.selectByPhoneOrCode(keyword.trim());
        // 脱敏：查询结果不回显完整手机号
        for (Complaint c : list) {
            c.setPhone(maskPhone(c.getPhone()));
        }
        return ApiResult.ok(list);
    }

    /** 消费者确认回复：需同时校验查询码，仅"处理中"状态可确认，提交后直接关闭工单 */
    @PostMapping("/complaint/{id}/confirm")
    public ApiResult<Void> confirmComplaint(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String consumerReply = body.getOrDefault("consumerReply", "").trim();
        String queryCode = body.getOrDefault("queryCode", "").trim();
        if (consumerReply.isEmpty()) {
            throw ApiException.badRequest("请填写确认内容");
        }
        if (queryCode.isEmpty()) {
            throw ApiException.badRequest("缺少查询码，无法确认");
        }
        Complaint c = complaintMapper.selectById(id);
        if (c == null) {
            throw ApiException.badRequest("投诉不存在");
        }
        if (complaintMapper.countByIdAndQueryCode(id, queryCode) == 0) {
            throw ApiException.badRequest("查询码不正确");
        }
        if (!"PROCESSING".equals(c.getStatus())) {
            throw ApiException.badRequest("该工单当前状态不支持确认");
        }
        complaintMapper.consumerReply(id, consumerReply);
        log.info("消费者确认投诉[{}]，工单已关闭", id);
        return ApiResult.ok();
    }

    /** 8 位查询码（去掉易混淆的 0/O/1/I） */
    private String generateQueryCode() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** 手机号脱敏：13812345678 → 138****5678 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? null : "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.length() > 50 ? ip.substring(0, 50) : ip;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
