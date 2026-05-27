package com.zyy.experiment;

import com.zyy.nl.NLIntent;
import com.zyy.nl.NLParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@DisplayName("实验一：NL解析准确率测试")
public class Experiment1NLParserAccuracyTest {

    private static final NLParser parser = new NLParser();

    enum Variant { STANDARD, COLLOQUIAL, ABBREVIATED, NOISY }

    record TestCase(String input, NLIntent expectedIntent, String expectedEntityType,
                    List<String> expectedEntityIds, Variant variant, String category) {}

    @Test
    @DisplayName("运行NL解析准确率实验")
    void runExperiment() throws IOException {
        List<TestCase> cases = buildAllTestCases();
        List<Experiment1Result> results = new ArrayList<>();

        for (TestCase tc : cases) {
            NLParser.ParseResult pr = parser.parse(tc.input);
            boolean intentMatch = pr.getIntent() == tc.expectedIntent;
            boolean entityTypeMatch = Objects.equals(pr.getEntityType(), tc.expectedEntityType);
            boolean entityIdsMatch = entityIdsEqual(pr.getEntityIds(), tc.expectedEntityIds);
            boolean overallMatch = intentMatch && entityTypeMatch && entityIdsMatch;

            results.add(new Experiment1Result(
                    tc.category, tc.variant.name(), tc.input,
                    tc.expectedIntent.name(), pr.getIntent() != null ? pr.getIntent().name() : "NULL",
                    intentMatch,
                    tc.expectedEntityType, pr.getEntityType(), entityTypeMatch,
                    String.join(";", tc.expectedEntityIds), String.join(";", pr.getEntityIds()), entityIdsMatch
            ));
        }

        // 汇总统计
        Map<String, CategoryStats> stats = computeStats(results);

        StringBuilder csv = new StringBuilder();
        csv.append("===== 实验一：NL解析准确率 =====\n\n");

        // 5x2 混淆矩阵（预测意图 vs 实际意图）
        csv.append("--- 1.1 意图识别 5x5 混淆矩阵 ---\n");
        csv.append(buildConfusionMatrix(results, "intent"));
        csv.append("\n");

        // 实体类型混淆矩阵
        csv.append("--- 1.2 实体类型识别 5x5 混淆矩阵 ---\n");
        csv.append(buildEntityTypeConfusionMatrix(results));
        csv.append("\n");

        // 分类准确率表
        csv.append("--- 1.3 分类准确率汇总 ---\n");
        csv.append("类别,样本数,意图准确率(%),实体类型准确率(%),实体ID准确率(%),综合准确率(%)\n");
        for (String cat : List.of("QUERY", "CREATE", "UPDATE", "DELETE", "STATISTICS")) {
            CategoryStats s = stats.get(cat);
            csv.append(String.format("%s,%d,%.1f,%.1f,%.1f,%.1f\n",
                    cat, s.total, s.intentAcc * 100, s.entityTypeAcc * 100,
                    s.entityIdAcc * 100, s.overallAcc * 100));
        }
        csv.append(String.format("总体,%d,%.1f,%.1f,%.1f,%.1f\n",
                results.size(),
                stats.values().stream().mapToDouble(s -> s.intentAcc).average().orElse(0) * 100,
                stats.values().stream().mapToDouble(s -> s.entityTypeAcc).average().orElse(0) * 100,
                stats.values().stream().mapToDouble(s -> s.entityIdAcc).average().orElse(0) * 100,
                stats.values().stream().mapToDouble(s -> s.overallAcc).average().orElse(0) * 100));

        // 变体准确率
        csv.append("\n--- 1.4 变体准确率对比 ---\n");
        csv.append("变体,样本数,意图准确率(%),实体类型准确率(%),实体ID准确率(%),综合准确率(%)\n");
        for (Variant v : Variant.values()) {
            List<Experiment1Result> vr = results.stream().filter(r -> r.variant.equals(v.name())).toList();
            double ia = vr.stream().filter(r -> r.intentMatch).count() * 100.0 / vr.size();
            double ea = vr.stream().filter(r -> r.entityTypeMatch).count() * 100.0 / vr.size();
            double ida = vr.stream().filter(r -> r.entityIdMatch).count() * 100.0 / vr.size();
            double oa = vr.stream().filter(r -> r.intentMatch && r.entityTypeMatch && r.entityIdMatch).count() * 100.0 / vr.size();
            csv.append(String.format("%s,%d,%.1f,%.1f,%.1f,%.1f\n", v.name(), vr.size(), ia, ea, ida, oa));
        }

        // 详细结果表
        csv.append("\n--- 1.5 逐条测试结果详情 ---\n");
        csv.append("类别,变体,输入文本,期望意图,实际意图,意图匹配,期望实体类型,实际实体类型,类型匹配,期望实体ID,实际实体ID,ID匹配\n");
        for (Experiment1Result r : results) {
            csv.append(String.format("%s,%s,\"%s\",%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    r.category, r.variant, r.input, r.expectedIntent, r.actualIntent, r.intentMatch,
                    nvl(r.expectedEntityType), nvl(r.actualEntityType), r.entityTypeMatch,
                    nvl(r.expectedEntityIds), nvl(r.actualEntityIds), r.entityIdMatch));
        }

        // 写入文件
        Path outPath = Paths.get("experiment_results", "experiment1_nl_accuracy_500.csv");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, csv.toString(), StandardCharsets.UTF_8);
        System.out.println("\n[实验一] 结果已写入: " + outPath.toAbsolutePath());
        System.out.println(csv);
    }

    // ==================== 500条测试用例（从 TestCases500 加载） ====================

    private List<TestCase> buildAllTestCases() {
        List<TestCase> all = new ArrayList<>();
        for (TestCases500.TestCase tc500 : TestCases500.ALL_CASES) {
            Variant v = Variant.valueOf(tc500.variantName());
            all.add(new TestCase(
                    tc500.input(),
                    tc500.expectedIntent(),
                    tc500.expectedEntityType(),
                    tc500.expectedEntityIds(),
                    v,
                    tc500.category()
            ));
        }
        return all;
    }

    // ==================== 统计工具 ====================

    private boolean entityIdsEqual(List<String> a, List<String> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        List<String> sa = new ArrayList<>(a); Collections.sort(sa);
        List<String> sb = new ArrayList<>(b); Collections.sort(sb);
        return sa.equals(sb);
    }

    private Map<String, CategoryStats> computeStats(List<Experiment1Result> results) {
        Map<String, CategoryStats> map = new LinkedHashMap<>();
        for (String cat : List.of("QUERY", "CREATE", "UPDATE", "DELETE", "STATISTICS")) {
            List<Experiment1Result> cr = results.stream().filter(r -> r.category.equals(cat)).toList();
            int t = cr.size();
            double ia = cr.stream().filter(r -> r.intentMatch).count() * 1.0 / t;
            double ea = cr.stream().filter(r -> r.entityTypeMatch).count() * 1.0 / t;
            double ida = cr.stream().filter(r -> r.entityIdMatch).count() * 1.0 / t;
            double oa = cr.stream().filter(r -> r.intentMatch && r.entityTypeMatch && r.entityIdMatch).count() * 1.0 / t;
            map.put(cat, new CategoryStats(t, ia, ea, ida, oa));
        }
        return map;
    }

    private String buildConfusionMatrix(List<Experiment1Result> results, String type) {
        List<String> labels = List.of("QUERY", "CREATE", "UPDATE", "DELETE", "STATISTICS");
        StringBuilder sb = new StringBuilder();
        sb.append("实际\\预测,").append(String.join(",", labels)).append("\n");
        for (String actual : labels) {
            sb.append(actual);
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String pred : labels) counts.put(pred, 0);
            for (Experiment1Result r : results) {
                if (r.category.equals(actual)) {
                    String pred = r.actualIntent;
                    if (counts.containsKey(pred)) counts.merge(pred, 1, Integer::sum);
                }
            }
            for (String pred : labels) sb.append(",").append(counts.get(pred));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildEntityTypeConfusionMatrix(List<Experiment1Result> results) {
        List<String> labels = List.of("EQUIPMENT", "CONSUMABLE", "INSPECTION", "NULL");
        StringBuilder sb = new StringBuilder();
        sb.append("实际\\预测,").append(String.join(",", labels)).append("\n");
        Map<String, String> catTypeMap = Map.of(
                "QUERY", "*", "CREATE", "*", "UPDATE", "*", "DELETE", "*", "STATISTICS", "*"
        );
        for (String actualType : labels) {
            sb.append(actualType);
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String pred : labels) counts.put(pred, 0);
            for (Experiment1Result r : results) {
                String at = r.expectedEntityType != null ? r.expectedEntityType : "NULL";
                if (at.equals(actualType)) {
                    String pt = r.actualEntityType != null ? r.actualEntityType : "NULL";
                    if (counts.containsKey(pt)) counts.merge(pt, 1, Integer::sum);
                }
            }
            for (String pred : labels) sb.append(",").append(counts.get(pred));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String nvl(String s) { return s != null ? s : "NULL"; }

    // ==================== 内部类 ====================

    record Experiment1Result(String category, String variant, String input,
                             String expectedIntent, String actualIntent, boolean intentMatch,
                             String expectedEntityType, String actualEntityType, boolean entityTypeMatch,
                             String expectedEntityIds, String actualEntityIds, boolean entityIdMatch) {}

    record CategoryStats(int total, double intentAcc, double entityTypeAcc, double entityIdAcc, double overallAcc) {}
}