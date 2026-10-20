package org.example.nongye.controller;

import org.example.nongye.common.PasswordHelper;
import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.SysUser;
import org.example.nongye.entity.TraceRecord;
import org.example.nongye.mapper.CropBatchMapper;
import org.example.nongye.mapper.SysUserMapper;
import org.example.nongye.mapper.TraceRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dealer")
public class DealerController {

    private static final Logger log = LoggerFactory.getLogger(DealerController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordHelper passwordHelper;

    private SysUser getCurrentUser(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user == null || !"dealer".equals(user.getRole())) {
            throw new RuntimeException("未登录");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        List<CropBatch> all = batchMapper.selectAll();
        List<CropBatch> available = all.stream()
                .filter(b -> "HARVESTED".equals(b.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("available", available);
        return "dealer/dashboard";
    }

    @GetMapping("/inbound")
    public String inboundPage(@RequestParam(required = false) String traceCode,
                              HttpSession session, Model model) {
        getCurrentUser(session);
        if (traceCode != null && !traceCode.isEmpty()) {
            model.addAttribute("batch", batchMapper.selectByTraceCode(traceCode));
        }
        return "dealer/inbound";
    }

    @PostMapping("/inbound")
    @Transactional(rollbackFor = Exception.class)
    public String doInbound(@RequestParam Long batchId, @RequestParam BigDecimal quantity,
                            HttpSession session) {
        SysUser user = getCurrentUser(session);
        CropBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        if (!"HARVESTED".equals(batch.getStatus())) {
            throw new RuntimeException("该批次当前状态不允许入库");
        }
        // 防止同一经销商对同一批次重复入库
        List<TraceRecord> existing = traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND");
        boolean alreadyInbound = existing.stream().anyMatch(r -> batchId.equals(r.getBatchId()));
        if (alreadyInbound) {
            throw new RuntimeException("该批次您已经入库过，请勿重复操作");
        }
        TraceRecord record = new TraceRecord();
        record.setBatchId(batchId);
        record.setFromUser(batch.getFarmerId());
        record.setToUser(user.getId());
        record.setType("INBOUND");
        record.setQuantity(quantity);
        record.setLocation(user.getAddress());
        record.setRemark("经销商收货入库");
        traceRecordMapper.insert(record);
        log.info("经销商[{}]入库批次[{}] 数量：{} 斤", user.getUsername(), batch.getBatchNo(), quantity);
        return "redirect:/dealer/inbound?success=1";
    }

    @GetMapping("/outbound")
    public String outboundPage(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        List<CropBatch> list = batchMapper.selectByDealerInvolved(user.getId());
        model.addAttribute("list", list);
        return "dealer/outbound";
    }

    @PostMapping("/outbound")
    @Transactional(rollbackFor = Exception.class)
    public String doOutbound(@RequestParam Long batchId, @RequestParam BigDecimal quantity,
                             @RequestParam String toName, @RequestParam String location,
                             HttpSession session) {
        SysUser user = getCurrentUser(session);
        CropBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        // 校验该经销商必须先入库过才能出库
        List<TraceRecord> inboundRecords = traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND");
        boolean hasInbound = inboundRecords.stream().anyMatch(r -> batchId.equals(r.getBatchId()));
        if (!hasInbound) {
            throw new RuntimeException("该批次尚未入库，不能出库");
        }
        TraceRecord record = new TraceRecord();
        record.setBatchId(batchId);
        record.setFromUser(user.getId());
        record.setType("OUTBOUND");
        record.setQuantity(quantity);
        record.setLocation(location);
        record.setRemark("发往：" + toName);
        traceRecordMapper.insert(record);
        log.info("经销商[{}]出库批次[{}] 发往：{} 数量：{} 斤", user.getUsername(), batch.getBatchNo(), toName, quantity);
        return "redirect:/dealer/outbound?success=1";
    }

    @GetMapping("/records")
    public String records(@RequestParam(required = false) String keyword,
                          HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("batches", batchMapper.searchByDealerInvolved(user.getId(), keyword));
        model.addAttribute("keyword", keyword);
        return "dealer/records";
    }

    // ==================== 入库/出库记录 ====================

    @GetMapping("/inbound-records")
    public String inboundRecords(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("records", traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND"));
        return "dealer/inbound_records";
    }

    @GetMapping("/outbound-records")
    public String outboundRecords(HttpSession session, Model model) {
        SysUser user = getCurrentUser(session);
        model.addAttribute("records", traceRecordMapper.selectByUserAndType(user.getId(), "OUTBOUND"));
        return "dealer/outbound_records";
    }

    // ==================== 溯源查询 ====================

    @GetMapping("/trace")
    public String traceQuery(@RequestParam(required = false) String traceCode,
                             HttpSession session, Model model) {
        getCurrentUser(session);
        if (traceCode != null && !traceCode.isEmpty()) {
            CropBatch batch = batchMapper.selectByTraceCode(traceCode.trim());
            model.addAttribute("batch", batch);
            model.addAttribute("traceCode", traceCode.trim());
        }
        return "dealer/trace";
    }

    // 个人中心已统一收敛到 ProfileController（/profile）
}
