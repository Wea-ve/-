package org.example.nongye.controller;

import org.example.nongye.common.PasswordHelper;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.example.nongye.util.LanIpUtil;
import org.example.nongye.util.QRCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/farmer")
public class FarmerController {

    private static final Logger log = LoggerFactory.getLogger(FarmerController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordHelper passwordHelper;

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${server.port:8080}")
    private int serverPort;

    private SysUser getCurrentUser(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user == null || !"farmer".equals(user.getRole())) {
            throw new RuntimeException("未登录");
        }
        return user;
    }

    /** 校验批次归属当前农户 */
    private CropBatch ownBatch(Long id, SysUser user) {
        CropBatch batch = batchMapper.selectById(id);
        if (batch == null || !batch.getFarmerId().equals(user.getId())) {
            throw new RuntimeException("无权限");
        }
        return batch;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        List<CropBatch> batches = batchMapper.selectByFarmerId(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("batches", batches);
        model.addAttribute("plantingCount", batches.stream().filter(b -> "PLANTING".equals(b.getStatus())).count());
        model.addAttribute("harvestedCount", batches.stream().filter(b -> "HARVESTED".equals(b.getStatus()) || "SOLD".equals(b.getStatus())).count());
        // 按作物汇总采收重量（饼图数据）
        java.util.Map<String, java.math.BigDecimal> cropStats = new java.util.LinkedHashMap<>();
        for (CropBatch b : batches) {
            if (b.getHarvestWeight() != null) {
                cropStats.merge(b.getCropName(), b.getHarvestWeight(), java.math.BigDecimal::add);
            }
        }
        model.addAttribute("cropStats", cropStats);
        return "farmer/dashboard";
    }

    @GetMapping("/batch")
    public String batchList(@RequestParam(required = false) String keyword,
                            HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("batches", batchMapper.searchByFarmer(user.getId(), keyword));
        model.addAttribute("keyword", keyword);
        return "farmer/batch_list";
    }

    @GetMapping("/batch/add")
    public String addBatchPage(HttpSession session) {
        getCurrentUser(session);
        return "farmer/batch_add";
    }

    @PostMapping("/batch/add")
    public String addBatch(CropBatch batch, HttpSession session) {
        SysUser user = getCurrentUser(session);
        batch.setFarmerId(user.getId());
        String batchNo = "BATCH" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        batch.setBatchNo(batchNo);
        batchMapper.insert(batch);
        log.info("农户[{}]新增种植批次：{}", user.getUsername(), batchNo);
        return "redirect:/farmer/batch";
    }

    @GetMapping("/batch/{id}/logs")
    public String batchLogs(@PathVariable Long id, HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        CropBatch batch = ownBatch(id, user);
        model.addAttribute("batch", batch);
        model.addAttribute("logs", farmingLogMapper.selectByBatchId(id));
        return "farmer/log_list";
    }

    @GetMapping("/batch/{id}/log/add")
    public String addLogPage(@PathVariable Long id, HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        ownBatch(id, user);
        model.addAttribute("batchId", id);
        return "farmer/log_add";
    }

    @PostMapping("/batch/{id}/log/add")
    public String addLog(@PathVariable Long id, HttpSession session, FarmingLog farmingLog) {
        SysUser user = getCurrentUser(session);
        CropBatch batch = ownBatch(id, user);
        farmingLog.setBatchId(id);
        farmingLogMapper.insert(farmingLog);
        log.info("农户[{}]为批次[{}]添加农事记录：{}", user.getUsername(), batch.getBatchNo(), farmingLog.getLogType());
        return "redirect:/farmer/batch/" + id + "/logs";
    }

    @GetMapping("/batch/{id}/harvest")
    public String harvestPage(@PathVariable Long id, HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("batch", ownBatch(id, user));
        return "farmer/batch_harvest";
    }

    @PostMapping("/batch/{id}/harvest")
    @Transactional(rollbackFor = Exception.class)
    public String harvest(@PathVariable Long id, @RequestParam String actualHarvestDate,
                          @RequestParam Double harvestWeight, HttpSession session) throws Exception {
        SysUser user = getCurrentUser(session);
        CropBatch batch = ownBatch(id, user);
        if (!"PLANTING".equals(batch.getStatus())) {
            throw new RuntimeException("当前批次状态不允许采收");
        }

        String traceCode = "TZ" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        batch.setTraceCode(traceCode);
        batch.setActualHarvestDate(LocalDate.parse(actualHarvestDate));
        batch.setHarvestWeight(java.math.BigDecimal.valueOf(harvestWeight));

        File qrDir = new File(uploadPath, "qrcode").getAbsoluteFile();
        qrDir.mkdirs();
        String qrFileName = "qr_" + traceCode + ".png";
        // 动态探测局域网 IP：换 WiFi / 热点后生成的二维码依然可扫
        String qrBaseUrl = LanIpUtil.getLanUrl(serverPort, appBaseUrl);
        QRCodeUtil.generateQRCode(qrBaseUrl + "/trace/" + traceCode,
                new File(qrDir, qrFileName).getAbsolutePath(), 300, 300);
        batch.setQrCodePath("/uploads/qrcode/" + qrFileName);

        batchMapper.harvest(batch);
        log.info("批次[{}]采收完成，溯源码：{}", batch.getBatchNo(), traceCode);

        TraceRecord record = new TraceRecord();
        record.setBatchId(id);
        record.setFromUser(user.getId());
        record.setType("HARVEST");
        record.setQuantity(java.math.BigDecimal.valueOf(harvestWeight));
        record.setLocation(batch.getLandLocation());
        record.setRemark("农户采收");
        traceRecordMapper.insert(record);

        return "redirect:/farmer/trace-codes";
    }

    // ==================== 农事管理（汇总） ====================

    @GetMapping("/logs")
    public String allLogs(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("logs", farmingLogMapper.selectByFarmer(user.getId()));
        model.addAttribute("batches", batchMapper.selectByFarmerId(user.getId()));
        return "farmer/logs";
    }

    // ==================== 采收溯源 ====================

    @GetMapping("/trace-codes")
    public String traceCodes(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("batches", batchMapper.selectByFarmerId(user.getId()));
        // 动态显示当前局域网地址，换网络自动更新
        model.addAttribute("baseUrl", LanIpUtil.getLanUrl(serverPort, appBaseUrl));
        return "farmer/trace_codes";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("reports", reportMapper.selectByFarmer(user.getId()));
        model.addAttribute("batches", batchMapper.selectByFarmerId(user.getId()));
        return "farmer/reports";
    }

    @PostMapping("/reports")
    public String uploadReport(@RequestParam Long batchId,
                               @RequestParam(required = false) String reportNo,
                               @RequestParam(required = false) String institution,
                               @RequestParam(required = false) String reportDate,
                               @RequestParam String result,
                               @RequestParam(required = false) MultipartFile file,
                               HttpSession session) throws Exception {
        SysUser user = getCurrentUser(session);
        ownBatch(batchId, user);

        DetectionReport report = new DetectionReport();
        report.setBatchId(batchId);
        report.setReportNo(reportNo);
        report.setInstitution(institution);
        report.setResult(result);
        if (reportDate != null && !reportDate.isEmpty()) {
            report.setReportDate(LocalDate.parse(reportDate));
        }

        // 保存检测报告文件（PDF/图片）
        if (file != null && !file.isEmpty()) {
            String original = file.getOriginalFilename();
            String ext = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            if (!ext.toLowerCase().matches("\\.(pdf|png|jpg|jpeg)")) {
                throw new RuntimeException("仅支持 PDF/PNG/JPG 文件");
            }
            File dir = new File(uploadPath, "reports").getAbsoluteFile();
            dir.mkdirs();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            // 必须用绝对路径：MultipartFile.transferTo 的相对路径会被解析到 Tomcat 临时目录
            file.transferTo(new File(dir, fileName).getAbsoluteFile());
            report.setReportUrl("/uploads/reports/" + fileName);
        }

        reportMapper.insert(report);
        log.info("农户[{}]上传批次[{}]检测报告，结果：{}", user.getUsername(), batchId, result);
        return "redirect:/farmer/reports";
    }

    // ==================== 销售记录 ====================

    @GetMapping("/sales")
    public String sales(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("records", traceRecordMapper.selectSalesByFarmer(user.getId()));
        return "farmer/sales";
    }

    // 个人中心已统一收敛到 ProfileController（/profile）
}
