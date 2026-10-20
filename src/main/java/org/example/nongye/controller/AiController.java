package org.example.nongye.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.example.nongye.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 问答：消费者扫码后基于批次数据回答问题
 * - 带图片 → 多模态：调通义千问 VL 模型识别照片（如农药包装、蔬菜状态）作答
 * - 纯文本 + 配置了 ai.api-key → Agent 模式：工具调用 + RAG 知识库 + 多轮会话
 * - 未配置 Key → 根据批次数据生成模拟回复（兜底，答辩演示够用）
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private AgentService agentService;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:qwen-plus}")
    private String model;

    /** 多模态视觉模型（识别消费者上传的照片） */
    @Value("${ai.vl-model:qwen-vl-max-latest}")
    private String vlModel;

    /**
     * POST /api/ai/chat
     * body: {"batchId": 1, "question": "打了几次农药？", "sessionId": "xxx", "image": "data:image/jpeg;base64,..."}
     * 返回：{"reply": "...", "tools": [{"label": "检测报告", "summary": "查询了 2 份检测报告"}]}
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long batchId = body.get("batchId") != null ? Long.valueOf(body.get("batchId").toString()) : null;
        String question = body.get("question") != null ? body.get("question").toString().trim() : "";
        String image = body.get("image") != null ? body.get("image").toString().trim() : "";
        String sessionId = body.get("sessionId") != null ? body.get("sessionId").toString() : "default";

        if (question.isEmpty() || batchId == null) {
            result.put("reply", "请输入您的问题");
            result.put("tools", new ArrayList<>());
            return result;
        }

        CropBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            result.put("reply", "批次信息不存在");
            result.put("tools", new ArrayList<>());
            return result;
        }

        try {
            if (!image.isEmpty()) {
                // 多模态：图片 + 问题 + 批次上下文 → VL 模型
                result.put("reply", callVlApi(batch, question, image));
                result.put("tools", new ArrayList<>());
            } else if (apiKey != null && !apiKey.isEmpty()) {
                // Agent 模式：工具调用 + RAG + 多轮
                Map<String, Object> agentResult = agentService.chat(batchId, sessionId, question);
                result.put("reply", agentResult.get("reply"));
                result.put("tools", agentResult.get("tools"));
            } else {
                // 无 Key 兜底：模拟回复
                List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batchId);
                List<TraceRecord> records = traceRecordMapper.selectByBatchId(batchId);
                List<DetectionReport> reports = reportMapper.selectByBatchId(batchId);
                result.put("reply", mockReply(question, batch, logs, records, reports));
                result.put("tools", new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("AI 问答处理失败", e);
            result.put("reply", "AI 服务暂时不可用，请稍后再试");
            result.put("tools", new ArrayList<>());
        }
        return result;
    }

    /** 多模态：调通义千问 VL（图片以 data URL 传入，OpenAI 兼容格式） */
    private String callVlApi(CropBatch batch, String question, String imageDataUrl) {
        // 图片转文字说明 + 批次上下文一起喂给模型
        String context = buildBriefContext(batch);

        JSONArray userContent = new JSONArray();
        userContent.add(new JSONObject().set("type", "text")
                .set("text", "消费者在溯源页上传了一张照片并提问：\n" + question
                        + "\n\n" + context
                        + "\n请结合图片内容回答（若是农药/肥料包装，说明该药剂用途、安全间隔期与注意事项；若是蔬菜照片，描述状态并给出判断或建议）。用中文简洁回答，控制在 300 字以内。"));
        userContent.add(new JSONObject().set("type", "image_url")
                .set("image_url", new JSONObject().set("url", imageDataUrl)));

        JSONObject body = new JSONObject();
        body.set("model", vlModel);
        body.set("messages", new Object[]{
                new JSONObject().set("role", "user").set("content", userContent)
        });

        try {
            HttpResponse resp = HttpRequest.post("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute();
            if (resp.isOk()) {
                return JSONUtil.parseObj(resp.body())
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getStr("content", "");
            }
            log.error("VL 模型返回非 200：{}", resp.body());
            return "图片识别服务暂时不可用，请稍后再试";
        } catch (Exception e) {
            log.error("调用 VL 模型失败", e);
            return "图片识别服务暂时不可用，请稍后再试";
        }
    }

    /** 批次简要上下文（多模态用，比完整版省 token） */
    private String buildBriefContext(CropBatch batch) {
        List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batch.getId());
        List<DetectionReport> reports = reportMapper.selectByBatchId(batch.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("该批次信息：作物=").append(batch.getCropName())
          .append("，产地=").append(batch.getLandLocation() == null ? "未记录" : batch.getLandLocation())
          .append("，采收日期=").append(batch.getActualHarvestDate() == null ? "未采收" : batch.getActualHarvestDate())
          .append("。\n");
        if (!logs.isEmpty()) {
            sb.append("近期农事记录：\n");
            int limit = Math.min(logs.size(), 5);
            for (int i = logs.size() - limit; i < logs.size(); i++) {
                FarmingLog l = logs.get(i);
                sb.append("- ").append(l.getLogDate()).append(" ").append(l.getMaterial() == null ? "" : l.getMaterial());
                sb.append("\n");
            }
        }
        if (!reports.isEmpty()) {
            sb.append("最新检测结论：").append(reports.get(0).getResult())
              .append("（").append(reports.get(0).getInstitution()).append("）\n");
        }
        return sb.toString();
    }

    /** 模拟回复：根据问题关键词匹配批次数据（无 API Key 时兜底） */
    private String mockReply(String question, CropBatch batch,
                              List<FarmingLog> logs, List<TraceRecord> records,
                              List<DetectionReport> reports) {
        String q = question.toLowerCase();

        // 农药相关
        if (q.contains("农药") || q.contains("打药") || q.contains("残留")) {
            long count = logs.stream().filter(l -> "PESTICIDE".equals(l.getLogType())).count();
            if (count == 0) return "该批次没有打药记录，属于生态种植。";
            StringBuilder sb = new StringBuilder("该批次共打药 " + count + " 次：\n");
            logs.stream().filter(l -> "PESTICIDE".equals(l.getLogType())).forEach(l -> {
                sb.append("• ").append(l.getLogDate());
                if (l.getMaterial() != null) sb.append(" 使用 ").append(l.getMaterial());
                if (l.getDosage() != null) sb.append("（").append(l.getDosage()).append("）");
                sb.append("\n");
            });
            if (!reports.isEmpty()) {
                DetectionReport r = reports.get(0);
                sb.append("检测报告结果：").append(r.getResult()).append("（").append(r.getInstitution()).append("）");
            }
            return sb.toString();
        }

        // 肥料相关
        if (q.contains("肥") || q.contains("施肥")) {
            long count = logs.stream().filter(l -> "FERTILIZE".equals(l.getLogType())).count();
            if (count == 0) return "该批次没有施肥记录。";
            StringBuilder sb = new StringBuilder("该批次共施肥 " + count + " 次：\n");
            logs.stream().filter(l -> "FERTILIZE".equals(l.getLogType())).forEach(l -> {
                sb.append("• ").append(l.getLogDate());
                if (l.getMaterial() != null) sb.append(" 使用 ").append(l.getMaterial());
                if (l.getDosage() != null) sb.append("（").append(l.getDosage()).append("）");
                sb.append("\n");
            });
            return sb.toString();
        }

        // 检测相关
        if (q.contains("检测") || q.contains("安全") || q.contains("合格")) {
            if (reports.isEmpty()) return "该批次暂无检测报告。";
            StringBuilder sb = new StringBuilder("检测报告信息：\n");
            for (DetectionReport r : reports) {
                sb.append("• 报告编号：").append(r.getReportNo()).append("\n");
                sb.append("• 检测机构：").append(r.getInstitution()).append("\n");
                sb.append("• 检测日期：").append(r.getReportDate()).append("\n");
                sb.append("• 检测结果：").append(r.getResult()).append("\n");
            }
            return sb.toString();
        }

        // 保存/储存
        if (q.contains("保存") || q.contains("储存") || q.contains("保鲜")) {
            String crop = batch.getCropName();
            if (crop.contains("西红柿") || crop.contains("番茄")) {
                return crop + "保存建议：未切开的常温放 3-5 天，放冰箱冷藏可存 1 周。切开后需密封冷藏并尽快食用。";
            }
            if (crop.contains("黄瓜")) {
                return crop + "保存建议：用保鲜膜包好放冰箱冷藏，可存 3-5 天。避免与乙烯含量高的水果同放。";
            }
            return crop + "保存建议：建议放冰箱冷藏保存，尽快食用保证新鲜。";
        }

        // 产地/哪里
        if (q.contains("产地") || q.contains("哪里") || q.contains("哪产") || q.contains("谁种")) {
            StringBuilder sb = new StringBuilder();
            sb.append("产地：").append(batch.getLandLocation() != null ? batch.getLandLocation() : "未记录").append("\n");
            sb.append("作物：").append(batch.getCropName());
            if (batch.getVariety() != null) sb.append("（").append(batch.getVariety()).append("）");
            sb.append("\n");
            sb.append("播种日期：").append(batch.getPlantDate());
            return sb.toString();
        }

        // 采收
        if (q.contains("采收") || q.contains("收获") || q.contains("摘")) {
            if (batch.getActualHarvestDate() == null) return "该批次尚未采收。";
            StringBuilder sb = new StringBuilder();
            sb.append("采收日期：").append(batch.getActualHarvestDate()).append("\n");
            if (batch.getHarvestWeight() != null) sb.append("采收重量：").append(batch.getHarvestWeight()).append("斤");
            return sb.toString();
        }

        // 流转/运输
        if (q.contains("运输") || q.contains("流转") || q.contains("物流") || q.contains("从哪")) {
            if (records.isEmpty()) return "暂无流转记录。";
            StringBuilder sb = new StringBuilder("该批次的流转路径：\n");
            for (TraceRecord r : records) {
                sb.append("• ").append(traceTypeName(r.getType()));
                if (r.getLocation() != null) sb.append("，").append(r.getLocation());
                if (r.getRemark() != null) sb.append("（").append(r.getRemark()).append("）");
                sb.append("\n");
            }
            return sb.toString();
        }

        // 默认：总结批次信息
        StringBuilder sb = new StringBuilder();
        sb.append("这是关于「").append(batch.getCropName()).append("」的溯源信息：\n");
        sb.append("• 产地：").append(batch.getLandLocation() != null ? batch.getLandLocation() : "未记录").append("\n");
        sb.append("• 播种：").append(batch.getPlantDate()).append("\n");
        if (batch.getActualHarvestDate() != null) {
            sb.append("• 采收：").append(batch.getActualHarvestDate());
            if (batch.getHarvestWeight() != null) sb.append("，").append(batch.getHarvestWeight()).append("斤");
            sb.append("\n");
        }
        sb.append("• 农事记录 ").append(logs.size()).append(" 条");
        if (!reports.isEmpty()) sb.append("，检测").append(reports.get(0).getResult());
        sb.append("\n\n您可以问我：打了几次农药？怎么保存？检测结果怎么样？");
        return sb.toString();
    }

    private String traceTypeName(String type) {
        return switch (type) {
            case "HARVEST" -> "采收";
            case "INBOUND" -> "入库";
            case "OUTBOUND" -> "出库";
            case "SALE" -> "销售";
            default -> type;
        };
    }
}
