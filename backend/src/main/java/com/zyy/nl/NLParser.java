package com.zyy.nl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * 自然语言解析器。
 * <p>基于规则识别用户意图（NLIntent）和实体ID。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Component
public class NLParser {

    private static final Pattern EQ_PATTERN = Pattern.compile("EQ-[\\d-]+");
    private static final Pattern CS_PATTERN = Pattern.compile("CS-[\\d-]+");
    private static final Pattern IN_PATTERN = Pattern.compile("IN-[\\d-]+");

    /**
     * 解析自然语言输入。
     *
     * @param input 用户原始输入
     * @return 解析结果
     */
    public ParseResult parse(String input) {
        if (input == null || input.isBlank()) {
            return new ParseResult(null, List.of(), null, 0.0);
        }

        IntentDetectionResult detection = detectIntent(input);
        List<String> entityIds = extractEntityIds(input);
        String entityType = detectEntityType(input);

        double confidence;
        NLIntent intent = detection.intent();
        if (intent == null) {
            intent = NLIntent.QUERY;
            confidence = 0.3;
        } else if (detection.singleKeyword()) {
            confidence = 0.8;
        } else {
            confidence = 1.0;
        }

        return new ParseResult(intent, entityIds, entityType, confidence);
    }

    /**
     * Intent detection result (immutable, thread-safe).
     * Replaces the former mutable instance field {@code matchedSingleKeyword}.
     */
    private record IntentDetectionResult(NLIntent intent, boolean singleKeyword) {}

    private IntentDetectionResult detectIntent(String text) {
        String t = text.toLowerCase();
        int matchCount = 0;
        NLIntent matched = null;

        if (t.contains("查询") || t.contains("查找") || t.contains("获取") || t.contains("看")) {
            matched = NLIntent.QUERY; matchCount++;
        }
        if (t.contains("创建") || t.contains("新增") || t.contains("添加") || t.contains("新增建")) {
            matched = NLIntent.CREATE; matchCount++;
        }
        if (t.contains("修改") || t.contains("更新") || t.contains("变更") || t.contains("编辑")) {
            matched = NLIntent.UPDATE; matchCount++;
        }
        if (t.contains("删除") || t.contains("移除")) {
            matched = NLIntent.DELETE; matchCount++;
        }
        if (t.contains("统计") || t.contains("汇总") || t.contains("数量") || t.contains("共")) {
            matched = NLIntent.STATISTICS; matchCount++;
        }

        if (matchCount == 0) {
            return new IntentDetectionResult(null, false);
        }
        return new IntentDetectionResult(matched, matchCount == 1);
    }

    /** 提取所有实体ID */
    private List<String> extractEntityIds(String text) {
        List<String> ids = new ArrayList<>();
        Matcher m;

        m = EQ_PATTERN.matcher(text);
        while (m.find()) { ids.add(m.group()); }

        m = CS_PATTERN.matcher(text);
        while (m.find()) { ids.add(m.group()); }

        m = IN_PATTERN.matcher(text);
        while (m.find()) { ids.add(m.group()); }

        return ids;
    }

    /** 根据实体ID前缀推断类型；无实体时返回null */
    private String detectEntityType(String text) {
        if (text.contains("EQ-") || text.contains("设备")) return "EQUIPMENT";
        if (text.contains("CS-") || text.contains("耗材")) return "CONSUMABLE";
        if (text.contains("IN-") || text.contains("巡检")) return "INSPECTION";
        return null;
    }

    // ── ParseResult 内部类 ─────────────────────────────────────

    public static class ParseResult {
        private final NLIntent intent;
        private final List<String> entityIds;
        private final String entityType;
        private final double confidence;
        private final String source;

        public ParseResult(NLIntent intent, List<String> entityIds, String entityType) {
            this(intent, entityIds, entityType, 0.0);
        }

        public ParseResult(NLIntent intent, List<String> entityIds, String entityType, double confidence) {
            this.intent = intent;
            this.entityIds = entityIds != null ? entityIds : List.of();
            this.entityType = entityType;
            this.confidence = confidence;
            this.source = "RULE";
        }

        private ParseResult(NLIntent intent, List<String> entityIds, String entityType,
                            double confidence, String source) {
            this.intent = intent;
            this.entityIds = entityIds != null ? entityIds : List.of();
            this.entityType = entityType;
            this.confidence = confidence;
            this.source = source;
        }

        public NLIntent getIntent()        { return intent; }
        public List<String> getEntityIds(){ return entityIds; }
        public String getEntityType()      { return entityType; }
        public double getConfidence()      { return confidence; }
        public String getSource()          { return source; }

        public ParseResult withSource(String source) {
            return new ParseResult(this.intent, this.entityIds, this.entityType, this.confidence, source);
        }

        public ParseResult withConfidence(double confidence) {
            return new ParseResult(this.intent, this.entityIds, this.entityType, confidence, this.source);
        }

        public String toString() {
            return "ParseResult{int=" + intent + ", ids=" + entityIds + ", type=" + entityType
                    + ", conf=" + String.format("%.2f", confidence) + ", src=" + source + "}";
        }
    }
}