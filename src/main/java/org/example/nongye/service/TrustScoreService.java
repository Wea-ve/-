package org.example.nongye.service;

import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.DetectionReport;
import org.example.nongye.entity.FarmingLog;
import org.example.nongye.entity.TraceRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 批次 AI 综合可信度评分引擎
 * 用现有登记数据聚合四个维度，输出 0-100 分与红黄绿三档，供扫码页"先看评分、再看明细"。
 * - 农事记录完整度 30 分：记录条数 + 农事类型覆盖度
 * - 检测结论 35 分：是否具备第三方检测报告及其结论
 * - 流转闭环 25 分：采收 → 入库 → 出库 → 销售 节点完整度
 * - 用药频次 10 分：打药次数，并复核末次打药距采收的安全间隔期
 */
@Service
public class TrustScoreService {

    /** 各维度满分，合计 100 */
    private static final int MAX_RECORD = 30;
    private static final int MAX_REPORT = 35;
    private static final int MAX_TRACE = 25;
    private static final int MAX_PESTICIDE = 10;

    /** 常见作物安全间隔期下限：末次打药距采收不足该天数时扣分并提示 */
    private static final int MIN_SAFETY_INTERVAL_DAYS = 3;

    /**
     * 计算批次综合可信度评分
     *
     * @param batch   批次信息（可为 null，缺采收日期时按流转节点判断）
     * @param logs    农事记录
     * @param records 流转记录
     * @param reports 检测报告（按检测日期倒序，首条为最新）
     * @return score / level / levelText / color / grade / headline / dimensions / advice / computedAt
     */
    public Map<String, Object> score(CropBatch batch, List<FarmingLog> logs,
                                     List<TraceRecord> records, List<DetectionReport> reports) {
        logs = logs == null ? List.of() : logs;
        records = records == null ? List.of() : records;
        reports = reports == null ? List.of() : reports;

        List<Map<String, Object>> dimensions = new ArrayList<>();
        List<String> advice = new ArrayList<>();

        int recordScore = scoreFarmingLogs(logs, dimensions, advice);
        int reportScore = scoreDetection(reports, dimensions, advice);
        int traceScore = scoreTraceChain(batch, records, dimensions, advice);
        int pesticideScore = scorePesticide(batch, logs, dimensions, advice);

        int total = recordScore + reportScore + traceScore + pesticideScore;
        String level;
        String levelText;
        String grade;
        String color;
        if (total >= 80) {
            level = "GREEN";
            levelText = "高可信";
            grade = "A";
            color = "#2e7d32";
        } else if (total >= 60) {
            level = "YELLOW";
            levelText = "基本可信";
            grade = "B";
            color = "#f9a825";
        } else {
            level = "RED";
            levelText = "待核实";
            grade = "C";
            color = "#d32f2f";
        }

        String headline = buildHeadline(reports, recordScore, traceScore);
        if (advice.isEmpty()) {
            advice.add("该批次登记数据较完整，可按常规方法清洗后食用");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", total);
        result.put("level", level);
        result.put("levelText", levelText);
        result.put("grade", grade);
        result.put("color", color);
        result.put("headline", headline);
        result.put("dimensions", dimensions);
        result.put("advice", advice.size() > 3 ? advice.subList(0, 3) : advice);
        result.put("computedAt", LocalDateTime.now().toString());
        return result;
    }

    /** 维度 1：农事记录完整度（30 分） */
    private int scoreFarmingLogs(List<FarmingLog> logs, List<Map<String, Object>> dimensions, List<String> advice) {
        int score;
        String detail;
        if (logs.isEmpty()) {
            score = 0;
            detail = "平台暂未登记农事记录";
            advice.add("建议农户补充播种、施肥、打药等农事记录");
        } else {
            Set<String> types = new HashSet<>();
            for (FarmingLog log : logs) {
                if (log.getLogType() != null) types.add(log.getLogType());
            }
            int covered = 0;
            for (String type : new String[]{"SOWING", "FERTILIZE", "PESTICIDE", "IRRIGATE", "WEED"}) {
                if (types.contains(type)) covered++;
            }
            int density = logs.size() >= 6 ? 8 : logs.size() >= 3 ? 5 : 2;
            score = Math.min(MAX_RECORD, 10 + Math.min(12, covered * 3) + density);
            detail = logs.size() + " 条记录，覆盖 " + covered + " 类农事";
            if (covered < 3) advice.add("农事记录类型偏少，建议补齐施肥/灌溉等关键环节");
        }
        dimensions.add(dimension("record", "农事记录完整度", score, MAX_RECORD, detail));
        return score;
    }

    /** 维度 2：检测结论（35 分） */
    private int scoreDetection(List<DetectionReport> reports, List<Map<String, Object>> dimensions, List<String> advice) {
        int score;
        String detail;
        if (reports.isEmpty()) {
            score = 0;
            detail = "暂无第三方检测报告";
            advice.add(0, "缺少第三方农残检测报告，建议上传或向商家索取");
        } else {
            DetectionReport latest = reports.get(0);
            String result = latest.getResult() == null ? "" : latest.getResult();
            boolean qualified = result.contains("合格") && !result.contains("不合格");
            if (qualified) {
                score = MAX_REPORT;
            } else if (result.contains("不合格")) {
                score = 5;
                advice.add(0, "最新检测结论为不合格，建议核实同批次产品并谨慎食用");
            } else {
                score = 18;
            }
            detail = "最新报告：" + (result.isEmpty() ? "结论未填写" : result)
                    + (latest.getReportDate() == null ? "" : "（" + latest.getReportDate() + "）");
        }
        dimensions.add(dimension("report", "检测结论", score, MAX_REPORT, detail));
        return score;
    }

    /** 维度 3：流转闭环（25 分） */
    private int scoreTraceChain(CropBatch batch, List<TraceRecord> records,
                               List<Map<String, Object>> dimensions, List<String> advice) {
        Set<String> types = new HashSet<>();
        for (TraceRecord record : records) {
            if (record.getType() != null) types.add(record.getType());
        }
        boolean harvested = (batch != null && batch.getActualHarvestDate() != null) || types.contains("HARVEST");
        boolean inbound = types.contains("INBOUND");
        boolean outbound = types.contains("OUTBOUND");
        boolean sale = types.contains("SALE");
        int score = (harvested ? 7 : 0) + (inbound ? 7 : 0) + (outbound ? 6 : 0) + (sale ? 5 : 0);

        String detail = "采收" + mark(harvested) + " 入库" + mark(inbound)
                + " 出库" + mark(outbound) + " 销售" + mark(sale);
        if (score < MAX_TRACE) {
            advice.add("流转链路仍有环节未登记，补齐后可信度更高");
        }
        dimensions.add(dimension("trace", "流转闭环", score, MAX_TRACE, detail));
        return score;
    }

    /** 维度 4：用药频次 + 安全间隔期（10 分） */
    private int scorePesticide(CropBatch batch, List<FarmingLog> logs,
                              List<Map<String, Object>> dimensions, List<String> advice) {
        long count = logs.stream().filter(l -> "PESTICIDE".equals(l.getLogType())).count();
        int score;
        String detail;
        if (count == 0) {
            score = 6;
            detail = "已登记记录中未见打药（记录可能不完整）";
        } else if (count <= 2) {
            score = MAX_PESTICIDE;
            detail = "打药 " + count + " 次";
        } else if (count <= 4) {
            score = 7;
            detail = "打药 " + count + " 次，频次偏多";
        } else {
            score = 4;
            detail = "打药 " + count + " 次，频次较高";
            advice.add("该批次打药次数较多，建议核对用药种类与安全间隔期");
        }

        LocalDate harvestDate = batch == null ? null : batch.getActualHarvestDate();
        LocalDate lastSprayDate = logs.stream()
                .filter(l -> "PESTICIDE".equals(l.getLogType()))
                .map(FarmingLog::getLogDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (count > 0 && harvestDate != null && lastSprayDate != null) {
            long days = ChronoUnit.DAYS.between(lastSprayDate, harvestDate);
            detail = detail + "；末次打药距采收 " + days + " 天";
            if (days < MIN_SAFETY_INTERVAL_DAYS) {
                score = Math.max(0, score - 4);
                detail = detail + "（低于常见 " + MIN_SAFETY_INTERVAL_DAYS + " 天安全间隔期）";
                advice.add("末次打药距采收过近，建议核实是否满足安全间隔期");
            }
        }
        dimensions.add(dimension("pesticide", "用药频次", score, MAX_PESTICIDE, detail));
        return score;
    }

    /** 一句话结论：优先说明检测结论，其次说明数据缺口 */
    private String buildHeadline(List<DetectionReport> reports, int recordScore, int traceScore) {
        if (reports.isEmpty()) {
            return "缺少第三方检测报告，当前可信度主要来自平台登记数据";
        }
        String result = reports.get(0).getResult() == null ? "" : reports.get(0).getResult();
        if (result.contains("不合格")) {
            return "最新检测结论存在风险，建议谨慎判断并核实批次";
        }
        if (recordScore >= 20 && traceScore >= 18) {
            return "检测合格，农事与流转数据较完整，可放心参考";
        }
        return "检测合格，但农事或流转数据仍有缺口，可信度待补强";
    }

    /** 统一维度结构 */
    private Map<String, Object> dimension(String key, String name, int score, int max, String detail) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("key", key);
        dim.put("name", name);
        dim.put("score", score);
        dim.put("max", max);
        dim.put("detail", detail);
        dim.put("status", score >= max * 0.85 ? "good" : score >= max * 0.55 ? "warn" : "bad");
        return dim;
    }

    private String mark(boolean present) {
        return present ? "✓" : "✗";
    }
}
