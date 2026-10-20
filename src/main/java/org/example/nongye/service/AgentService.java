package org.example.nongye.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Agent 服务：基于通义千问 function calling 的多轮对话
 * - 工具集：批次信息 / 农事记录 / 检测报告 / 流转链路 / 知识库检索（全部限定当前批次，防越权）
 * - 循环：模型返回 tool_calls → 服务端执行并把结果回喂 → 最多 MAX_ROUNDS 轮
 * - 多轮会话：ConcurrentHashMap 按 sessionId 保留最近 20 条消息
 * - 返回：{ reply, tools: [{label, summary}] } 供前端展示"已调用工具"徽章
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final String CHAT_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    /** 单次提问最多几轮工具调用 */
    private static final int MAX_ROUNDS = 5;
    /** 每个会话保留的最大消息条数 */
    private static final int MAX_HISTORY = 20;
    /** 会话数量上限，防止内存无限增长 */
    private static final int MAX_SESSIONS = 200;

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private KnowledgeService knowledgeService;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.agent-model:qwen-plus}")
    private String agentModel;

    /** 多轮会话存储：sessionId → 消息历史（含 tool 消息） */
    private final ConcurrentHashMap<String, List<JSONObject>> sessions = new ConcurrentHashMap<>();

    /**
     * Agent 多轮对话入口
     * @return reply 回复文本；tools 本轮实际调用过的工具列表
     */
    public Map<String, Object> chat(Long batchId, String sessionId, String question) {
        List<Map<String, String>> toolsUsed = new ArrayList<>();
        String reply;

        List<JSONObject> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(new JSONObject().set("role", "user").set("content", question));

        try {
            JSONArray tools = buildToolSchemas();

            for (int round = 0; round < MAX_ROUNDS; round++) {
                JSONObject respMsg = callChat(buildSystemPrompt(batchId), history, tools);
                if (respMsg == null) {
                    reply = "AI 服务暂时不可用，请稍后再试";
                    // 失败时移除刚加入的用户消息，避免污染下一轮
                    history.remove(history.size() - 1);
                    return result(reply, toolsUsed);
                }

                JSONArray toolCalls = respMsg.getJSONArray("tool_calls");
                if (toolCalls == null || toolCalls.isEmpty()) {
                    // 没有工具调用 → 模型给出最终回答，结束循环
                    reply = respMsg.getStr("content", "");
                    if (reply == null || reply.isEmpty()) {
                        log.warn("模型返回空回复，message={}", respMsg);
                        reply = "抱歉，我暂时没能理解您的问题，请换个问法试试";
                    }
                    history.add(new JSONObject().set("role", "assistant").set("content", reply));
                    trimHistory(history);
                    return result(reply, toolsUsed);
                }

                // 记录 assistant 的 tool_calls 消息（只保留标准字段，剔除 reasoning_content 等额外字段），再逐个执行工具
                history.add(new JSONObject()
                        .set("role", "assistant")
                        .set("content", respMsg.getStr("content"))
                        .set("tool_calls", toolCalls));
                for (int i = 0; i < toolCalls.size(); i++) {
                    JSONObject call = toolCalls.getJSONObject(i);
                    JSONObject fn = call.getJSONObject("function");
                    String name = fn.getStr("name");
                    String args = fn.getStr("arguments", "{}");

                    String toolResult = executeTool(name, args, batchId, toolsUsed);
                    history.add(new JSONObject()
                            .set("role", "tool")
                            .set("tool_call_id", call.getStr("id"))
                            .set("content", toolResult));
                }
            }
            // 超过轮数上限，强制让模型基于已查到的数据作答
            JSONObject respMsg = callChat(buildSystemPrompt(batchId) + "\n（不要再调用工具，请直接根据以上工具结果回答）", history, null);
            reply = respMsg != null ? respMsg.getStr("content", "") : "AI 服务暂时不可用，请稍后再试";
            if (reply == null || reply.isEmpty()) {
                log.warn("轮数上限后模型仍返回空回复，message={}", respMsg);
                reply = "抱歉，我暂时没能理解您的问题，请换个问法试试";
            }
            history.add(new JSONObject().set("role", "assistant").set("content", reply));
            trimHistory(history);
            return result(reply, toolsUsed);
        } catch (Exception e) {
            log.error("Agent 对话异常", e);
            reply = "AI 服务暂时不可用，请稍后再试";
            return result(reply, toolsUsed);
        }
    }

    /** 调用 chat/completions（OpenAI 兼容） */
    private JSONObject callChat(String systemPrompt, List<JSONObject> history, JSONArray tools) {
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", systemPrompt));
        history.forEach(messages::add);

        JSONObject body = new JSONObject()
                .set("model", agentModel)
                .set("messages", messages)
                .set("temperature", 0.3);
        if (tools != null) body.set("tools", tools);

        try {
            HttpResponse resp = HttpRequest.post(CHAT_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute();
            if (!resp.isOk()) {
                log.error("Agent 接口返回非 200：{}", resp.body());
                return null;
            }
            return JSONUtil.parseObj(resp.body())
                    .getJSONArray("choices").getJSONObject(0).getJSONObject("message");
        } catch (Exception e) {
            log.error("调用 Agent 接口失败", e);
            return null;
        }
    }

    /** 执行一个工具调用，结果作为 tool 消息回喂模型 */
    private String executeTool(String name, String argsJson, Long batchId, List<Map<String, String>> toolsUsed) {
        JSONObject args;
        try {
            args = JSONUtil.parseObj(argsJson);
        } catch (Exception e) {
            args = new JSONObject();
        }
        String summary;
        String data;

        switch (name) {
            case "get_batch_info": {
                CropBatch b = batchMapper.selectById(batchId);
                data = b == null ? "批次不存在" : JSONUtil.toJsonStr(batchInfo(b));
                summary = "查询了批次基础信息";
                break;
            }
            case "get_farming_logs": {
                List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batchId);
                data = logs.isEmpty() ? "该批次暂无农事记录" : JSONUtil.toJsonStr(logs);
                summary = "查询了 " + logs.size() + " 条农事记录";
                break;
            }
            case "get_detection_reports": {
                List<DetectionReport> reports = reportMapper.selectByBatchId(batchId);
                data = reports.isEmpty() ? "该批次暂无检测报告" : JSONUtil.toJsonStr(reports);
                summary = "查询了 " + reports.size() + " 份检测报告";
                break;
            }
            case "get_trace_records": {
                List<TraceRecord> records = traceRecordMapper.selectByBatchId(batchId);
                data = records.isEmpty() ? "该批次暂无流转记录" : JSONUtil.toJsonStr(records);
                summary = "查询了 " + records.size() + " 条流转记录";
                break;
            }
            case "search_knowledge": {
                String query = args.getStr("query", "");
                List<Map<String, Object>> hits = knowledgeService.search(query, 3);
                if (hits.isEmpty()) {
                    data = "知识库未命中相关内容";
                    summary = "知识库未命中：" + query;
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Map<String, Object> hit : hits) {
                        sb.append("【").append(hit.get("title")).append("】")
                          .append(hit.get("content")).append("\n");
                    }
                    data = sb.toString();
                    summary = "检索到「" + hits.get(0).get("title") + "」等 " + hits.size() + " 条知识";
                }
                break;
            }
            default:
                data = "未知工具：" + name;
                summary = "调用未知工具 " + name;
        }

        Map<String, String> used = new HashMap<>();
        used.put("label", toolLabel(name));
        used.put("summary", summary);
        toolsUsed.add(used);
        log.info("Agent 工具调用 batch={} tool={} -> {}", batchId, name, summary);
        return data;
    }

    /** 工具中文名（前端徽章展示用） */
    private String toolLabel(String name) {
        switch (name) {
            case "get_batch_info": return "批次信息";
            case "get_farming_logs": return "农事记录";
            case "get_detection_reports": return "检测报告";
            case "get_trace_records": return "流转链路";
            case "search_knowledge": return "知识库检索";
            default: return name;
        }
    }

    /** 工具 schema 定义（OpenAI function calling 格式） */
    private JSONArray buildToolSchemas() {
        JSONObject emptyParams = new JSONObject().set("type", "object").set("properties", new JSONObject());

        JSONObject searchParams = new JSONObject()
                .set("type", "object")
                .set("properties", new JSONObject()
                        .set("query", new JSONObject()
                                .set("type", "string")
                                .set("description", "要检索的知识主题，如：农药残留限量、安全间隔期、储存方法、清洗技巧")));
        searchParams.set("required", new Object[]{"query"});

        JSONArray tools = new JSONArray();
        tools.add(tool("get_batch_info", "查询当前批次的种植信息：作物、品种、产地、播种/采收日期、采收重量、状态", emptyParams));
        tools.add(tool("get_farming_logs", "查询该批次全部农事记录：打药/施肥/灌溉/采收等，含日期、物料、用量。回答'打过几次药''施了什么肥'等问题前必须调用", emptyParams));
        tools.add(tool("get_detection_reports", "查询该批次的第三方农残检测报告：编号、机构、日期、结论。回答'检测合格吗''农残超标吗'等问题前必须调用", emptyParams));
        tools.add(tool("get_trace_records", "查询该批次的流转链路：入库/出库/运输环节与地点。回答'从哪来的''经过哪些环节'等问题前必须调用", emptyParams));
        tools.add(tool("search_knowledge", "检索农事知识库（GB2763 农残限量、安全间隔期、储存保鲜、清洗技巧等通用知识）。涉及标准限值、保存方法等专业知识时调用", searchParams));
        return tools;
    }

    private JSONObject tool(String name, String description, JSONObject parameters) {
        return new JSONObject().set("type", "function").set("function", new JSONObject()
                .set("name", name)
                .set("description", description)
                .set("parameters", parameters));
    }

    /** system prompt：角色 + 批次概要 + 行为规范 */
    private String buildSystemPrompt(Long batchId) {
        CropBatch b = batchMapper.selectById(batchId);
        StringBuilder sb = new StringBuilder();
        sb.append("你是农产品溯源平台的消费者智能助手。今天日期：").append(LocalDate.now()).append("。\n");
        if (b != null) {
            sb.append("当前批次概要：作物=").append(b.getCropName())
              .append("，产地=").append(b.getLandLocation() == null ? "未填写" : b.getLandLocation())
              .append("，批次号=").append(b.getBatchNo()).append("。\n");
        }
        sb.append("行为规范：\n")
          .append("1. 涉及该批次的具体数据（打药、施肥、检测、流转）必须先调用对应工具查询，禁止凭空编造；\n")
          .append("2. 涉及农残限量标准、安全间隔期、储存方法、清洗技巧等通用知识时，必须先调用 search_knowledge 检索知识库，再结合检索结果回答；\n")
          .append("3. 用中文回答，语气对消费者友好，重点内容分点列出，控制在 300 字以内；\n")
          .append("4. 检测结论为'合格'时可放心推荐，未检出报告时如实说明。\n");
        return sb.toString();
    }

    /** 批次信息精简 map（工具返回值，去掉无用字段省 token） */
    private Map<String, Object> batchInfo(CropBatch b) {
        Map<String, Object> m = new HashMap<>();
        m.put("batchNo", b.getBatchNo());
        m.put("cropName", b.getCropName());
        m.put("variety", b.getVariety());
        m.put("landLocation", b.getLandLocation());
        m.put("plantDate", b.getPlantDate());
        m.put("actualHarvestDate", b.getActualHarvestDate());
        m.put("harvestWeight", b.getHarvestWeight());
        m.put("status", b.getStatus());
        return m;
    }

    /** 会话过长时裁剪：从头删除，直到首条是 user 消息，保证工具配对完整 */
    private void trimHistory(List<JSONObject> history) {
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        while (!history.isEmpty() && !"user".equals(history.get(0).getStr("role"))) {
            history.remove(0);
        }
    }

    /** 统一响应结构 */
    private Map<String, Object> result(String reply, List<Map<String, String>> toolsUsed) {
        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("tools", toolsUsed);
        return result;
    }
}
