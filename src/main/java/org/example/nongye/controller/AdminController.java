package org.example.nongye.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.nongye.common.PasswordHelper;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.example.nongye.util.ExcelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private ScanLogMapper scanLogMapper;
    @Autowired
    private PasswordHelper passwordHelper;

    private SysUser checkAdmin(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user == null || !"admin".equals(user.getRole())) {
            throw new RuntimeException("无权限");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        checkAdmin(session);
        List<Map<String, Object>> cropStats = batchMapper.countByCrop();
        List<Complaint> complaints = complaintMapper.selectAll();
        long pendingCount = complaints.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
        model.addAttribute("cropStats", cropStats);
        model.addAttribute("complaintTotal", complaints.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("batchTotal", batchMapper.selectAll().size());
        // 投诉状态分布（饼图）
        model.addAttribute("complaintStatus", complaintMapper.countByStatus());
        // 近 7 天流转量（折线图）
        model.addAttribute("trace7days", traceRecordMapper.countByDay(7));
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String userList(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String role,
                           HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("users", userMapper.searchUsers(keyword, role));
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        // 各角色账号数量（用于标签徽标）
        model.addAttribute("allCount", userMapper.selectAllUsers().size());
        model.addAttribute("farmerCount", userMapper.selectByRole("farmer").size());
        model.addAttribute("dealerCount", userMapper.selectByRole("dealer").size());
        return "admin/users";
    }

    /**
     * 新增账号表单页（仅可创建农户/经销商）
     */
    @GetMapping("/users/add")
    public String userAddForm(HttpSession session) {
        checkAdmin(session);
        return "admin/user_form";
    }

    /**
     * 处理新增账号
     */
    @PostMapping("/users/add")
    public String userAdd(@RequestParam String username, @RequestParam String password,
                          @RequestParam String realName, @RequestParam String role,
                          @RequestParam(required = false) String phone,
                          @RequestParam(required = false) String address,
                          HttpSession session, Model model) {
        checkAdmin(session);

        // 服务端二次校验：角色只能是农户或经销商，禁止通过菜单创建管理员
        if (!"farmer".equals(role) && !"dealer".equals(role)) {
            model.addAttribute("error", "非法的账号角色");
            return "admin/user_form";
        }
        String uname = username == null ? "" : username.trim();
        if (uname.length() < 3) {
            model.addAttribute("error", "账号长度至少 3 位");
            return "admin/user_form";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "初始密码长度至少 6 位");
            return "admin/user_form";
        }
        if (userMapper.selectByUsername(uname) != null) {
            model.addAttribute("error", "账号已存在，请更换");
            return "admin/user_form";
        }

        SysUser user = new SysUser();
        user.setUsername(uname);
        user.setPassword(passwordHelper.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setPhone(phone);
        user.setAddress(address);
        userMapper.insert(user);
        log.info("管理员创建新账号：username={}, role={}", uname, role);
        return "redirect:/admin/users";
    }

    @GetMapping("/complaints")
    public String complaintList(@RequestParam(required = false) String keyword,
                                HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("complaints", complaintMapper.search(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/complaints";
    }

    @PostMapping("/complaints/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String reply,
                        @RequestParam String status, HttpSession session) {
        SysUser admin = checkAdmin(session);
        if (reply == null || reply.trim().isEmpty()) {
            throw new RuntimeException("回复内容不能为空");
        }
        Complaint c = new Complaint();
        c.setId(id);
        c.setReply(reply);
        c.setStatus(status);
        complaintMapper.reply(c);
        log.info("管理员[{}]回复投诉[{}] 状态：{}", admin.getUsername(), id, status);
        return "redirect:/admin/complaints";
    }

    // ==================== 生产监管 ====================

    @GetMapping("/batches")
    public String batches(@RequestParam(required = false) String keyword,
                          HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("batches", batchMapper.search(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/batches";
    }

    @GetMapping("/farming-logs")
    public String farmingLogs(HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("logs", farmingLogMapper.selectAllWithBatch());
        return "admin/farming_logs";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        checkAdmin(session);
        List<DetectionReport> reports = reportMapper.selectAllWithBatch();
        model.addAttribute("reports", reports);
        model.addAttribute("qualifiedCount", reports.stream().filter(r -> "合格".equals(r.getResult())).count());
        model.addAttribute("unqualifiedCount", reports.stream().filter(r -> "不合格".equals(r.getResult())).count());
        return "admin/reports";
    }

    // ==================== 流通监管 ====================

    @GetMapping("/trace-records")
    public String traceRecords(@RequestParam(required = false) String keyword,
                               HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("records", traceRecordMapper.search(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/trace_records";
    }

    @GetMapping("/trace")
    public String traceQuery(@RequestParam(required = false) String traceCode,
                             HttpSession session, Model model) {
        checkAdmin(session);
        if (traceCode != null && !traceCode.isEmpty()) {
            CropBatch batch = batchMapper.selectByTraceCode(traceCode.trim());
            model.addAttribute("batch", batch);
            model.addAttribute("traceCode", traceCode.trim());
        }
        return "admin/trace";
    }

    // ==================== Excel 导出 ====================

    /**
     * 导出批次 Excel
     */
    @GetMapping("/batches/export")
    public void exportBatches(HttpSession session, HttpServletResponse response) throws Exception {
        checkAdmin(session);
        List<CropBatch> batches = batchMapper.selectAll();
        String[] headers = {"批次号", "作物", "品种", "产地", "农户", "播种日期", "采收日期", "采收重量(斤)", "状态"};
        List<List<Object>> rows = new ArrayList<>();
        for (CropBatch b : batches) {
            List<Object> row = new ArrayList<>();
            row.add(b.getBatchNo());
            row.add(b.getCropName());
            row.add(b.getVariety());
            row.add(b.getLandLocation());
            row.add(b.getFarmerName());
            row.add(b.getPlantDate());
            row.add(b.getActualHarvestDate());
            row.add(b.getHarvestWeight());
            row.add("PLANTING".equals(b.getStatus()) ? "种植中" :
                    ("HARVESTED".equals(b.getStatus()) ? "已采收" : "已售罄"));
            rows.add(row);
        }
        writeExcel(response, "种植批次", headers, rows, "batches.xlsx");
    }

    /**
     * 导出投诉 Excel
     */
    @GetMapping("/complaints/export")
    public void exportComplaints(HttpSession session, HttpServletResponse response) throws Exception {
        checkAdmin(session);
        List<Complaint> complaints = complaintMapper.selectAll();
        String[] headers = {"ID", "投诉人", "电话", "类型", "内容", "关联批次", "作物", "状态", "回复", "创建时间"};
        List<List<Object>> rows = new ArrayList<>();
        for (Complaint c : complaints) {
            List<Object> row = new ArrayList<>();
            row.add(c.getId());
            row.add(c.getComplainant());
            row.add(c.getPhone());
            row.add(c.getType());
            row.add(c.getContent());
            row.add(c.getBatchNo());
            row.add(c.getCropName());
            row.add("PENDING".equals(c.getStatus()) ? "待处理" :
                    ("PROCESSING".equals(c.getStatus()) ? "处理中" : "已关闭"));
            row.add(c.getReply());
            row.add(c.getCreateTime());
            rows.add(row);
        }
        writeExcel(response, "投诉工单", headers, rows, "complaints.xlsx");
    }

    private void writeExcel(HttpServletResponse response, String sheetName, String[] headers,
                            List<List<Object>> rows, String fileName) throws Exception {
        byte[] data = ExcelUtil.export(sheetName, headers, rows);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    // ==================== 扫码统计 ====================

    @GetMapping("/scan-stats")
    public String scanStats(HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("todayCount", scanLogMapper.countToday());
        model.addAttribute("scan7days", scanLogMapper.countLast7Days());
        model.addAttribute("hotBatches", scanLogMapper.hotBatches());
        model.addAttribute("scanLogs", scanLogMapper.selectAll());
        return "admin/scan_stats";
    }

    // 个人中心已统一收敛到 ProfileController（/profile）
}
