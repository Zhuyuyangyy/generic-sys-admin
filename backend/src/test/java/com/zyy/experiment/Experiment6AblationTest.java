package com.zyy.experiment;

import cn.hutool.json.JSONUtil;
import com.zyy.nl.ExecuteResult;
import com.zyy.nl.NLService;
import com.zyy.nl.NLParseResult;
import com.zyy.nl.CausalDAGService;
import com.zyy.nl.CausalGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.sql.init.mode=never",
        "mybatis-plus.mapper-locations=",
        "spring.redis.enabled=false"
    }
)
@ActiveProfiles("test")
@DisplayName("【实验六】Ablation Study - 消融实验")
public class Experiment6AblationTest {

    @Autowired
    private NLService nlService;

    @Autowired
    private CausalDAGService causalDAGService;

    private static final String CSV_OUTPUT_PATH = "experiment_results/experiment6_ablation.csv";

    private static final int TEST_ITERATIONS = 100;

    private static class AblationResult {
        String config;
        double intentAccuracy;
        double overallAccuracy;
        double avgLatency;
        int impactWarnings;
        int totalTests;

        String[] toCSVRow() {
            String intentAcc = intentAccuracy >= 0 ? String.format("%.1f%%", intentAccuracy * 100) : "N/A";
            String overallAcc = overallAccuracy >= 0 ? String.format("%.1f%%", overallAccuracy * 100) : "N/A";
            return new String[]{
                config,
                intentAcc,
                overallAcc,
                String.format("%.2f", avgLatency) + "ms",
                String.valueOf(impactWarnings)
            };
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        Path dir = Paths.get("experiment_results");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    @Test
    @DisplayName("Config A: NL Only (enableCausal=false)")
    void configA_NLOnly() {
        nlService.setEnableCausal(false);
        
        List<String> testCommands = nlService.getTestCommands();
        List<ExecuteResult> results = new ArrayList<>();
        
        long totalLatency = 0;
        int highImpactCount = 0;
        int correctIntents = 0;
        
        String[] expectedIntents = {"DELETE", "UPDATE", "QUERY", "UPDATE", "DELETE", "QUERY", "CREATE", "UPDATE", "QUERY", "DELETE"};
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String command = testCommands.get(i % testCommands.size());
            ExecuteResult result = nlService.executeWithCausalCheck(command);
            results.add(result);
            
            totalLatency += result.getLatencyMs();
            if (result.isCausalCheckPerformed()) {
                if (result.isHasHighImpact()) {
                    highImpactCount++;
                }
            }
            
            if (i < expectedIntents.length && result.getIntent().equals(expectedIntents[i])) {
                correctIntents++;
            }
        }
        
        double intentAccuracy = correctIntents / (double) Math.min(TEST_ITERATIONS, expectedIntents.length);
        double avgLatency = totalLatency / (double) TEST_ITERATIONS;
        
        AblationResult ablationResult = new AblationResult();
        ablationResult.config = "A: NL Only";
        ablationResult.intentAccuracy = intentAccuracy;
        ablationResult.overallAccuracy = intentAccuracy;
        ablationResult.avgLatency = avgLatency;
        ablationResult.impactWarnings = 0;
        ablationResult.totalTests = TEST_ITERATIONS;
        
        System.out.println("\n========================================");
        System.out.println("Config A: NL Only (enableCausal=false)");
        System.out.println("========================================");
        System.out.println("意图准确率: " + String.format("%.1f%%", intentAccuracy * 100));
        System.out.println("综合准确率: " + String.format("%.1f%%", intentAccuracy * 100));
        System.out.println("平均响应时间: " + String.format("%.2fms", avgLatency));
        System.out.println("影响预警数: 0");
        System.out.println("总测试数: " + TEST_ITERATIONS);
        
        nlService.setEnableCausal(true);
        
        saveAblationResult(ablationResult);
    }

    @Test
    @DisplayName("Config B: Full System (enableCausal=true)")
    void configB_FullSystem() {
        nlService.setEnableCausal(true);
        
        List<String> testCommands = nlService.getTestCommands();
        List<ExecuteResult> results = new ArrayList<>();
        
        long totalLatency = 0;
        int highImpactCount = 0;
        int correctIntents = 0;
        
        String[] expectedIntents = {"DELETE", "UPDATE", "QUERY", "UPDATE", "DELETE", "QUERY", "CREATE", "UPDATE", "QUERY", "DELETE"};
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String command = testCommands.get(i % testCommands.size());
            ExecuteResult result = nlService.executeWithCausalCheck(command);
            results.add(result);
            
            totalLatency += result.getLatencyMs();
            if (result.isCausalCheckPerformed()) {
                if (result.isHasHighImpact()) {
                    highImpactCount++;
                }
            }
            
            if (i < expectedIntents.length && result.getIntent().equals(expectedIntents[i])) {
                correctIntents++;
            }
        }
        
        double intentAccuracy = correctIntents / (double) Math.min(TEST_ITERATIONS, expectedIntents.length);
        double avgLatency = totalLatency / (double) TEST_ITERATIONS;
        
        AblationResult ablationResult = new AblationResult();
        ablationResult.config = "B: Full System";
        ablationResult.intentAccuracy = intentAccuracy;
        ablationResult.overallAccuracy = intentAccuracy;
        ablationResult.avgLatency = avgLatency;
        ablationResult.impactWarnings = highImpactCount;
        ablationResult.totalTests = TEST_ITERATIONS;
        
        System.out.println("\n========================================");
        System.out.println("Config B: Full System (enableCausal=true)");
        System.out.println("========================================");
        System.out.println("意图准确率: " + String.format("%.1f%%", intentAccuracy * 100));
        System.out.println("综合准确率: " + String.format("%.1f%%", intentAccuracy * 100));
        System.out.println("平均响应时间: " + String.format("%.2fms", avgLatency));
        System.out.println("影响预警数: " + highImpactCount);
        System.out.println("总测试数: " + TEST_ITERATIONS);
        
        appendAblationResult(ablationResult);
    }

    @Test
    @DisplayName("Config C: Causal Check Only")
    void configC_CausalOnly() {
        List<Long> latencies = new ArrayList<>();
        
        String[] entityTypes = {"EQUIPMENT", "CONSUMABLE", "USER", "INVENTORY"};
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String entityType = entityTypes[i % entityTypes.length];
            
            long start = System.currentTimeMillis();
            CausalGraph graph = causalDAGService.predictImpactDirect(entityType);
            long latency = System.currentTimeMillis() - start;
            
            latencies.add(latency);
        }
        
        double avgLatency = latencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        AblationResult ablationResult = new AblationResult();
        ablationResult.config = "C: Causal Only";
        ablationResult.intentAccuracy = -1;
        ablationResult.overallAccuracy = -1;
        ablationResult.avgLatency = avgLatency;
        ablationResult.impactWarnings = -1;
        ablationResult.totalTests = TEST_ITERATIONS;
        
        System.out.println("\n========================================");
        System.out.println("Config C: Causal Check Only");
        System.out.println("========================================");
        System.out.println("意图准确率: N/A");
        System.out.println("综合准确率: N/A");
        System.out.println("平均响应时间: " + String.format("%.2fms", avgLatency));
        System.out.println("影响预警数: N/A");
        System.out.println("总测试数: " + TEST_ITERATIONS);
        
        appendAblationResult(ablationResult);
    }

    @Test
    @DisplayName("【完整实验】运行所有配置并生成对比报告")
    void runAllConfigsAndGenerateReport() {
        List<AblationResult> allResults = new ArrayList<>();
        
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║            Experiment 6: Ablation Study - Full Run              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
        System.out.println("测试迭代次数: " + TEST_ITERATIONS);
        System.out.println();
        
        nlService.setEnableCausal(false);
        AblationResult resultA = runConfigA();
        allResults.add(resultA);
        
        nlService.setEnableCausal(true);
        AblationResult resultB = runConfigB();
        allResults.add(resultB);
        
        AblationResult resultC = runConfigC();
        allResults.add(resultC);
        
        printComparisonTable(resultA, resultB, resultC);
        
        saveAblationCSV(allResults);
        
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Ablation Study Complete                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
        System.out.println("Output: " + CSV_OUTPUT_PATH);
    }

    private AblationResult runConfigA() {
        nlService.setEnableCausal(false);
        
        List<String> testCommands = nlService.getTestCommands();
        long totalLatency = 0;
        int correctIntents = 0;
        
        String[] expectedIntents = {"DELETE", "UPDATE", "QUERY", "UPDATE", "DELETE", "QUERY", "CREATE", "UPDATE", "QUERY", "DELETE"};
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String command = testCommands.get(i % testCommands.size());
            ExecuteResult result = nlService.executeWithCausalCheck(command);
            totalLatency += result.getLatencyMs();
            
            if (i < expectedIntents.length && result.getIntent().equals(expectedIntents[i])) {
                correctIntents++;
            }
        }
        
        double intentAccuracy = correctIntents / (double) Math.min(TEST_ITERATIONS, expectedIntents.length);
        
        AblationResult r = new AblationResult();
        r.config = "A: NL Only";
        r.intentAccuracy = intentAccuracy;
        r.overallAccuracy = intentAccuracy;
        r.avgLatency = totalLatency / (double) TEST_ITERATIONS;
        r.impactWarnings = 0;
        
        System.out.println("[Config A] NL Only - Intent Acc: " + String.format("%.1f%%", intentAccuracy * 100) 
            + ", Avg Latency: " + String.format("%.2fms", r.avgLatency));
        
        return r;
    }

    private AblationResult runConfigB() {
        nlService.setEnableCausal(true);
        
        List<String> testCommands = nlService.getTestCommands();
        long totalLatency = 0;
        int highImpactCount = 0;
        int correctIntents = 0;
        
        String[] expectedIntents = {"DELETE", "UPDATE", "QUERY", "UPDATE", "DELETE", "QUERY", "CREATE", "UPDATE", "QUERY", "DELETE"};
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String command = testCommands.get(i % testCommands.size());
            ExecuteResult result = nlService.executeWithCausalCheck(command);
            totalLatency += result.getLatencyMs();
            
            if (result.isCausalCheckPerformed() && result.isHasHighImpact()) {
                highImpactCount++;
            }
            
            if (i < expectedIntents.length && result.getIntent().equals(expectedIntents[i])) {
                correctIntents++;
            }
        }
        
        double intentAccuracy = correctIntents / (double) Math.min(TEST_ITERATIONS, expectedIntents.length);
        
        AblationResult r = new AblationResult();
        r.config = "B: Full System";
        r.intentAccuracy = intentAccuracy;
        r.overallAccuracy = intentAccuracy;
        r.avgLatency = totalLatency / (double) TEST_ITERATIONS;
        r.impactWarnings = highImpactCount;
        
        System.out.println("[Config B] Full System - Intent Acc: " + String.format("%.1f%%", intentAccuracy * 100) 
            + ", Avg Latency: " + String.format("%.2fms", r.avgLatency)
            + ", Impact Warnings: " + highImpactCount);
        
        return r;
    }

    private AblationResult runConfigC() {
        List<Long> latencies = new ArrayList<>();
        String[] entityTypes = {"EQUIPMENT", "CONSUMABLE", "USER", "INVENTORY"};
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String entityType = entityTypes[i % entityTypes.length];
            
            long start = System.currentTimeMillis();
            causalDAGService.predictImpactDirect(entityType);
            latencies.add(System.currentTimeMillis() - start);
        }
        
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        
        AblationResult r = new AblationResult();
        r.config = "C: Causal Only";
        r.intentAccuracy = -1;
        r.overallAccuracy = -1;
        r.avgLatency = avgLatency;
        r.impactWarnings = -1;
        
        System.out.println("[Config C] Causal Only - Avg Latency: " + String.format("%.2fms", avgLatency));
        
        return r;
    }

    private void printComparisonTable(AblationResult a, AblationResult b, AblationResult c) {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        Ablation Study Results                          │");
        System.out.println("├──────────────────┬───────────┬───────────┬─────────────┬───────────────┤");
        System.out.println("│ Config           │ Intent Acc│ Overall   │ Avg Latency │ Impact Warning│");
        System.out.println("├──────────────────┼───────────┼───────────┼─────────────┼───────────────┤");
        
        String intentA = a.intentAccuracy >= 0 ? String.format("%.1f%%", a.intentAccuracy * 100) : "N/A";
        String intentB = b.intentAccuracy >= 0 ? String.format("%.1f%%", b.intentAccuracy * 100) : "N/A";
        String overallA = a.overallAccuracy >= 0 ? String.format("%.1f%%", a.overallAccuracy * 100) : "N/A";
        String overallB = b.overallAccuracy >= 0 ? String.format("%.1f%%", b.overallAccuracy * 100) : "N/A";
        
        System.out.printf("│ A: NL Only       │ %-9s │ %-9s │ %-11s │ %-14d │%n", 
            intentA, overallA, String.format("%.2fms", a.avgLatency), 0);
        System.out.printf("│ B: Full System   │ %-9s │ %-9s │ %-11s │ %-14d │%n", 
            intentB, overallB, String.format("%.2fms", b.avgLatency), b.impactWarnings);
        System.out.printf("│ C: Causal Only   │ N/A      │ N/A      │ %-11s │ N/A          │%n", 
            String.format("%.2fms", c.avgLatency));
        
        System.out.println("└──────────────────┴───────────┴───────────┴─────────────┴───────────────┘");
    }

    private void saveAblationResult(AblationResult result) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT_PATH))) {
            writer.println("Config,Intent Acc,Overall Acc,Avg Latency,Impact Warnings");
            writer.println(String.join(",",
                result.config,
                result.intentAccuracy >= 0 ? String.format("%.1f%%", result.intentAccuracy * 100) : "N/A",
                result.overallAccuracy >= 0 ? String.format("%.1f%%", result.overallAccuracy * 100) : "N/A",
                String.format("%.2fms", result.avgLatency),
                String.valueOf(result.impactWarnings)
            ));
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    private void appendAblationResult(AblationResult result) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT_PATH, true))) {
            writer.println(String.join(",",
                result.config,
                result.intentAccuracy >= 0 ? String.format("%.1f%%", result.intentAccuracy * 100) : "N/A",
                result.overallAccuracy >= 0 ? String.format("%.1f%%", result.overallAccuracy * 100) : "N/A",
                String.format("%.2fms", result.avgLatency),
                String.valueOf(result.impactWarnings)
            ));
        } catch (IOException e) {
            System.err.println("Failed to append to CSV: " + e.getMessage());
        }
    }

    private void saveAblationCSV(List<AblationResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT_PATH))) {
            writer.println("Config,Intent Acc,Overall Acc,Avg Latency,Impact Warnings");
            for (AblationResult r : results) {
                writer.println(String.join(",",
                    r.config,
                    r.intentAccuracy >= 0 ? String.format("%.1f%%", r.intentAccuracy * 100) : "N/A",
                    r.overallAccuracy >= 0 ? String.format("%.1f%%", r.overallAccuracy * 100) : "N/A",
                    String.format("%.2fms", r.avgLatency),
                    r.impactWarnings >= 0 ? String.valueOf(r.impactWarnings) : "N/A"
                ));
            }
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }
}
