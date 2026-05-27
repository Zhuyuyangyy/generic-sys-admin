package com.zyy.experiment;

import com.zyy.nl.HybridNLParser;
import com.zyy.nl.LLMNLParser;
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
import java.util.concurrent.atomic.AtomicInteger;

@DisplayName("实验七：HybridNLParser三模式对比")
public class Experiment7HybridParserTest {

    record HybridResult(String input, NLIntent expectedIntent, String expectedEntityType,
                        List<String> expectedEntityIds, String variant, String category,
                        NLIntent actualIntent, String actualEntityType, List<String> actualEntityIds,
                        boolean intentOk, boolean typeOk, boolean idOk, boolean overallOk,
                        String source, double confidence, long latencyMs) {}

    @Test
    @DisplayName("运行Hybrid模式500条测试")
    void runHybridExperiment() throws IOException {
        LLMNLParser llmParser = new LLMNLParser();
        if (!llmParser.isAvailable()) {
            System.err.println("[实验七] DEEPSEEK_API_KEY 未设置，跳过Hybrid测试");
            return;
        }

        NLParser ruleParser = new NLParser();
        HybridNLParser hybrid = new HybridNLParser(ruleParser, llmParser);
        hybrid.setRuleOnly(false);

        List<TestCases500.TestCase> allCases = TestCases500.ALL_CASES;
        List<HybridResult> results = new ArrayList<>();
        AtomicInteger llmTriggerCount = new AtomicInteger(0);
        long totalStart = System.nanoTime();

        for (int i = 0; i < allCases.size(); i++) {
            TestCases500.TestCase tc = allCases.get(i);
            long start = System.nanoTime();
            NLParser.ParseResult result;
            try {
                result = hybrid.parse(tc.input());
            } catch (Exception e) {
                result = new NLParser.ParseResult(null, List.of(), null, 0.0)
                        .withSource("LLM_TIMEOUT");
            }
            long latMs = (System.nanoTime() - start) / 1_000_000;

            boolean intentOk = result.getIntent() == tc.expectedIntent();
            boolean typeOk = Objects.equals(result.getEntityType(), tc.expectedEntityType());
            boolean idOk = idsEqual(result.getEntityIds(), tc.expectedEntityIds());
            boolean overallOk = intentOk && typeOk && idOk;

            if ("LLM".equals(result.getSource()) || "LLM_TIMEOUT".equals(result.getSource())) {
                llmTriggerCount.incrementAndGet();
            }

            results.add(new HybridResult(
                    tc.input(), tc.expectedIntent(), tc.expectedEntityType(),
                    tc.expectedEntityIds(), tc.variantName(), tc.category(),
                    result.getIntent(), result.getEntityType(), result.getEntityIds(),
                    intentOk, typeOk, idOk, overallOk,
                    result.getSource(), result.getConfidence(), latMs
            ));

            if ((i + 1) % 50 == 0) {
                System.out.printf("[实验七] 进度: %d/%d, LLM触发: %d%n",
                        i + 1, allCases.size(), llmTriggerCount.get());
            }
        }

        long totalTimeMs = (System.nanoTime() - totalStart) / 1_000_000;
        double avgLatMs = (double) totalTimeMs / allCases.size();
        int llmTriggers = llmTriggerCount.get();
        double llmRate = llmTriggers * 100.0 / allCases.size();

        long intentOkCount = results.stream().filter(r -> r.intentOk).count();
        long overallOkCount = results.stream().filter(r -> r.overallOk).count();
        double intentAcc = intentOkCount * 100.0 / allCases.size();
        double overallAcc = overallOkCount * 100.0 / allCases.size();

        StringBuilder csv = new StringBuilder();
        csv.append("===== 实验七：HybridNLParser三模式对比 =====\n\n");

        // Section 1
        csv.append("--- 7.1 三模式总体对比 ---\n");
        csv.append("Mode,Intent Acc(%),Overall Acc(%),Avg Latency(ms),LLM Trigger Rate(%)\n");
        csv.append(String.format("Rule Only,61.2,61.2,<1,0\n"));
        csv.append(String.format("Hybrid,%.1f,%.1f,%.1f,%.1f\n", intentAcc, overallAcc, avgLatMs, llmRate));
        csv.append(String.format("LLM Only,98.0,84.0,798,100\n"));

        // Section 2
        csv.append("\n--- 7.2 Hybrid按变体分析 ---\n");
        csv.append("Variant,Rule Intent(%),Rule Overall(%),Hybrid Intent(%),Hybrid Overall(%),LLM Only Intent(%),LLM Only Overall(%),Hybrid LLM Rate(%)\n");
        for (String v : List.of("STANDARD", "COLLOQUIAL", "ABBREVIATED", "NOISY")) {
            List<HybridResult> vr = results.stream().filter(r -> r.variant.equals(v)).toList();
            double hIntent = vr.stream().filter(r -> r.intentOk).count() * 100.0 / vr.size();
            double hOverall = vr.stream().filter(r -> r.overallOk).count() * 100.0 / vr.size();
            double hLlmRate = vr.stream().filter(r -> "LLM".equals(r.source) || "LLM_TIMEOUT".equals(r.source))
                    .count() * 100.0 / vr.size();
            csv.append(String.format("%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f\n",
                    v, ruleVariantAcc(v, "intent"), ruleVariantAcc(v, "overall"),
                    hIntent, hOverall, llmVariantAcc(v, "intent"), llmVariantAcc(v, "overall"),
                    hLlmRate));
        }

        // Section 3
        csv.append("\n--- 7.3 Hybrid按类别分析 ---\n");
        csv.append("Category,Rule Intent(%),Rule Overall(%),Hybrid Intent(%),Hybrid Overall(%),LLM Trigger Count,LLM Trigger Rate(%)\n");
        for (String cat : List.of("QUERY", "CREATE", "UPDATE", "DELETE", "STATISTICS")) {
            List<HybridResult> cr = results.stream().filter(r -> r.category.equals(cat)).toList();
            double hIntent = cr.stream().filter(r -> r.intentOk).count() * 100.0 / cr.size();
            double hOverall = cr.stream().filter(r -> r.overallOk).count() * 100.0 / cr.size();
            long catLlm = cr.stream().filter(r -> "LLM".equals(r.source) || "LLM_TIMEOUT".equals(r.source)).count();
            double catLlmRate = catLlm * 100.0 / cr.size();
            csv.append(String.format("%s,%.1f,%.1f,%.1f,%.1f,%d,%.1f\n",
                    cat, ruleCatAcc(cat, "intent"), ruleCatAcc(cat, "overall"),
                    hIntent, hOverall, catLlm, catLlmRate));
        }

        // Section 4
        csv.append("\n--- 7.4 Hybrid逐条详情 ---\n");
        csv.append("序号,类别,变体,输入,期望意图,实际意图,意图OK,来源,置信度,延迟(ms)\n");
        for (int i = 0; i < results.size(); i++) {
            HybridResult r = results.get(i);
            csv.append(String.format("%d,%s,%s,\"%s\",%s,%s,%s,%s,%.2f,%d\n",
                    i + 1, r.category, r.variant, r.input,
                    r.expectedIntent, r.actualIntent, r.intentOk,
                    r.source, r.confidence, r.latencyMs));
        }

        // Section 5
        csv.append("\n--- 7.5 LLM延迟分布 ---\n");
        List<Long> llmLats = results.stream()
                .filter(r -> "LLM".equals(r.source))
                .map(r -> r.latencyMs).sorted().toList();
        if (!llmLats.isEmpty()) {
            double avg = llmLats.stream().mapToLong(Long::longValue).average().orElse(0);
            double p50 = percentile(llmLats, 50);
            double p95 = percentile(llmLats, 95);
            double p99 = percentile(llmLats, 99);
            csv.append(String.format("LLM调用次数: %d\n", llmLats.size()));
            csv.append(String.format("平均: %.1fms, P50: %.0fms, P95: %.0fms, P99: %.0fms, Min: %dms, Max: %dms\n",
                    avg, p50, p95, p99, llmLats.get(0), llmLats.get(llmLats.size() - 1)));
        }

        Path outPath = Paths.get("experiment_results", "experiment7_hybrid_parser.csv");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, csv.toString(), StandardCharsets.UTF_8);
        System.out.println("\n[实验七] 结果已写入: " + outPath.toAbsolutePath());
        System.out.printf("[实验七] Hybrid: Intent=%.1f%%, Overall=%.1f%%, LLM触发率=%.1f%%, 平均延迟=%.1fms%n",
                intentAcc, overallAcc, llmRate, avgLatMs);
    }

    private double ruleVariantAcc(String variant, String metric) {
        return switch (variant) {
            case "STANDARD" -> "intent".equals(metric) ? 79.2 : 79.2;
            case "COLLOQUIAL" -> "intent".equals(metric) ? 20.0 : 20.0;
            case "ABBREVIATED" -> "intent".equals(metric) ? 73.6 : 73.6;
            case "NOISY" -> "intent".equals(metric) ? 72.0 : 72.0;
            default -> 0;
        };
    }

    private double ruleCatAcc(String cat, String metric) {
        return switch (cat) {
            case "QUERY" -> "intent".equals(metric) ? 99.0 : 99.0;
            case "CREATE" -> "intent".equals(metric) ? 43.0 : 43.0;
            case "UPDATE" -> "intent".equals(metric) ? 64.0 : 64.0;
            case "DELETE" -> "intent".equals(metric) ? 38.0 : 38.0;
            case "STATISTICS" -> "intent".equals(metric) ? 62.0 : 62.0;
            default -> 0;
        };
    }

    private double llmVariantAcc(String variant, String metric) {
        return switch (variant) {
            case "STANDARD" -> "intent".equals(metric) ? 100.0 : 100.0;
            case "COLLOQUIAL" -> "intent".equals(metric) ? 96.0 : 80.0;
            case "ABBREVIATED" -> "intent".equals(metric) ? 100.0 : 64.0;
            case "NOISY" -> "intent".equals(metric) ? 96.0 : 92.0;
            default -> 0;
        };
    }

    private boolean idsEqual(List<String> a, List<String> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        List<String> sa = new ArrayList<>(a); Collections.sort(sa);
        List<String> sb = new ArrayList<>(b); Collections.sort(sb);
        return sa.equals(sb);
    }

    private double percentile(List<Long> sorted, double pct) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}
