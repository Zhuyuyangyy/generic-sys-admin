package com.zyy.experiment;

import cn.hutool.json.JSONUtil;
import com.zyy.nl.ExecuteResult;
import com.zyy.nl.NLService;
import com.zyy.nl.NLParseResult;
import com.zyy.nl.CausalDAGService;
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
import java.util.Map;

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
@DisplayName("【实验五】Case Study - 因果检查场景分析")
public class Experiment5CaseStudyTest {

    @Autowired
    private NLService nlService;

    @Autowired
    private CausalDAGService causalDAGService;

    private static final String CSV_OUTPUT_PATH = "experiment_results/experiment5_case_study.csv";

    private static class ScenarioResult {
        String scenario;
        String input;
        String intent;
        String entityId;
        boolean causalCheckPerformed;
        int impactedNodesCount;
        boolean hasHighImpact;
        long latencyMs;
        String fullResponse;

        String[] toCSVRow() {
            return new String[]{
                scenario,
                input,
                intent,
                entityId,
                String.valueOf(causalCheckPerformed),
                String.valueOf(impactedNodesCount),
                String.valueOf(hasHighImpact),
                String.valueOf(latencyMs)
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
    @DisplayName("场景1: 删除高影响设备")
    void scenario1_deleteHighImpactEquipment() {
        String input = "删除设备EQ-2024-001";
        
        ExecuteResult result = nlService.executeWithCausalCheck(input);
        
        ScenarioResult scenarioResult = new ScenarioResult();
        scenarioResult.scenario = "删除高影响设备";
        scenarioResult.input = input;
        scenarioResult.intent = result.getIntent();
        scenarioResult.entityId = result.getEntityId();
        scenarioResult.causalCheckPerformed = result.isCausalCheckPerformed();
        scenarioResult.impactedNodesCount = result.getImpactedNodesCount() != null ? result.getImpactedNodesCount() : 0;
        scenarioResult.hasHighImpact = result.isHasHighImpact();
        scenarioResult.latencyMs = result.getLatencyMs();
        scenarioResult.fullResponse = result.getRawResponse();
        
        System.out.println("\n========================================");
        System.out.println("场景1: 删除高影响设备");
        System.out.println("========================================");
        System.out.println("输入: " + input);
        System.out.println("意图: " + result.getIntent());
        System.out.println("实体ID: " + result.getEntityId());
        System.out.println("触发因果检查: " + result.isCausalCheckPerformed());
        System.out.println("影响节点数: " + scenarioResult.impactedNodesCount);
        System.out.println("hasHighImpact: " + result.isHasHighImpact());
        System.out.println("响应时间: " + result.getLatencyMs() + "ms");
        System.out.println("完整JSON响应:");
        System.out.println(JSONUtil.toJsonPretty(result.getRawResponse()));
        
        assertNotNull(result.getIntent());
        assertTrue(result.isCausalCheckPerformed());
        
        saveToCSV(List.of(scenarioResult));
    }

    @Test
    @DisplayName("场景2: 更新耗材")
    void scenario2_updateConsumable() {
        String input = "修改耗材CS-2024-008的库存数量";
        
        ExecuteResult result = nlService.executeWithCausalCheck(input);
        
        ScenarioResult scenarioResult = new ScenarioResult();
        scenarioResult.scenario = "更新耗材";
        scenarioResult.input = input;
        scenarioResult.intent = result.getIntent();
        scenarioResult.entityId = result.getEntityId();
        scenarioResult.causalCheckPerformed = result.isCausalCheckPerformed();
        scenarioResult.impactedNodesCount = result.getImpactedNodesCount() != null ? result.getImpactedNodesCount() : 0;
        scenarioResult.hasHighImpact = result.isHasHighImpact();
        scenarioResult.latencyMs = result.getLatencyMs();
        scenarioResult.fullResponse = result.getRawResponse();
        
        System.out.println("\n========================================");
        System.out.println("场景2: 更新耗材");
        System.out.println("========================================");
        System.out.println("输入: " + input);
        System.out.println("意图: " + result.getIntent());
        System.out.println("实体ID: " + result.getEntityId());
        System.out.println("触发因果检查: " + result.isCausalCheckPerformed());
        System.out.println("影响节点数: " + scenarioResult.impactedNodesCount);
        System.out.println("hasHighImpact: " + result.isHasHighImpact());
        System.out.println("响应时间: " + result.getLatencyMs() + "ms");
        System.out.println("完整JSON响应:");
        System.out.println(JSONUtil.toJsonPretty(result.getRawResponse()));
        
        assertNotNull(result.getIntent());
        assertTrue(result.isCausalCheckPerformed());
        
        appendToCSV(List.of(scenarioResult));
    }

    @Test
    @DisplayName("场景3: 查询操作（不触发因果检查）")
    void scenario3_queryOperation() {
        String input = "查询设备EQ-2024-001的状态";
        
        ExecuteResult result = nlService.executeWithCausalCheck(input);
        
        ScenarioResult scenarioResult = new ScenarioResult();
        scenarioResult.scenario = "查询操作";
        scenarioResult.input = input;
        scenarioResult.intent = result.getIntent();
        scenarioResult.entityId = result.getEntityId();
        scenarioResult.causalCheckPerformed = result.isCausalCheckPerformed();
        scenarioResult.impactedNodesCount = result.getImpactedNodesCount() != null ? result.getImpactedNodesCount() : 0;
        scenarioResult.hasHighImpact = result.isHasHighImpact();
        scenarioResult.latencyMs = result.getLatencyMs();
        scenarioResult.fullResponse = result.getRawResponse();
        
        System.out.println("\n========================================");
        System.out.println("场景3: 查询操作（不触发因果检查）");
        System.out.println("========================================");
        System.out.println("输入: " + input);
        System.out.println("意图: " + result.getIntent());
        System.out.println("实体ID: " + result.getEntityId());
        System.out.println("触发因果检查: " + result.isCausalCheckPerformed());
        System.out.println("影响节点数: " + scenarioResult.impactedNodesCount);
        System.out.println("hasHighImpact: " + result.isHasHighImpact());
        System.out.println("响应时间: " + result.getLatencyMs() + "ms");
        System.out.println("完整JSON响应:");
        System.out.println(JSONUtil.toJsonPretty(result.getRawResponse()));
        
        assertNotNull(result.getIntent());
        assertFalse(result.isCausalCheckPerformed(), "查询操作不应触发因果检查");
        
        appendToCSV(List.of(scenarioResult));
    }

    @Test
    @DisplayName("【完整实验】运行所有场景并生成CSV报告")
    void runAllScenariosAndGenerateReport() {
        List<ScenarioResult> allResults = new ArrayList<>();
        
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          Experiment 5: Case Study - Full Run            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        String[] scenarios = {
            "删除设备EQ-2024-001",
            "修改耗材CS-2024-008的库存数量",
            "查询设备EQ-2024-001的状态"
        };
        
        String[] scenarioNames = {"删除高影响设备", "更新耗材", "查询操作"};
        
        for (int i = 0; i < scenarios.length; i++) {
            String input = scenarios[i];
            ExecuteResult result = nlService.executeWithCausalCheck(input);
            
            ScenarioResult sr = new ScenarioResult();
            sr.scenario = scenarioNames[i];
            sr.input = input;
            sr.intent = result.getIntent();
            sr.entityId = result.getEntityId();
            sr.causalCheckPerformed = result.isCausalCheckPerformed();
            sr.impactedNodesCount = result.getImpactedNodesCount() != null ? result.getImpactedNodesCount() : 0;
            sr.hasHighImpact = result.isHasHighImpact();
            sr.latencyMs = result.getLatencyMs();
            sr.fullResponse = result.getRawResponse();
            
            allResults.add(sr);
            
            System.out.println("\n----------------------------------------");
            System.out.println("场景" + (i + 1) + ": " + scenarioNames[i]);
            System.out.println("----------------------------------------");
            System.out.println("  输入: " + input);
            System.out.println("  意图: " + result.getIntent());
            System.out.println("  实体ID: " + result.getEntityId());
            System.out.println("  因果检查: " + (result.isCausalCheckPerformed() ? "是" : "否"));
            if (result.isCausalCheckPerformed()) {
                System.out.println("  影响节点数: " + sr.impactedNodesCount);
                System.out.println("  高影响: " + (result.isHasHighImpact() ? "是" : "否"));
            }
            System.out.println("  延迟: " + result.getLatencyMs() + "ms");
        }
        
        saveToCSV(allResults);
        
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    CSV Report Generated                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("Output: " + CSV_OUTPUT_PATH);
    }

    private void saveToCSV(List<ScenarioResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT_PATH))) {
            writer.println("场景,输入,意图,实体ID,触发因果检查,影响节点数,hasHighImpact,响应时间(ms)");
            for (ScenarioResult r : results) {
                writer.println(String.join(",",
                    "\"" + r.scenario + "\"",
                    "\"" + r.input + "\"",
                    r.intent,
                    r.entityId,
                    String.valueOf(r.causalCheckPerformed),
                    String.valueOf(r.impactedNodesCount),
                    String.valueOf(r.hasHighImpact),
                    String.valueOf(r.latencyMs)
                ));
            }
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    private void appendToCSV(List<ScenarioResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT_PATH, true))) {
            for (ScenarioResult r : results) {
                writer.println(String.join(",",
                    "\"" + r.scenario + "\"",
                    "\"" + r.input + "\"",
                    r.intent,
                    r.entityId,
                    String.valueOf(r.causalCheckPerformed),
                    String.valueOf(r.impactedNodesCount),
                    String.valueOf(r.hasHighImpact),
                    String.valueOf(r.latencyMs)
                ));
            }
        } catch (IOException e) {
            System.err.println("Failed to append to CSV: " + e.getMessage());
        }
    }
}
