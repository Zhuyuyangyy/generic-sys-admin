package com.zyy.experiment;

import com.zyy.nl.LLMNLParser;
import com.zyy.nl.NLIntent;
import com.zyy.nl.NLParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@DisplayName("实验四：DeepSeek V4 Flash vs 规则引擎基线对比")
public class Experiment4LLMBaselineTest {

    private static final NLParser ruleParser = new NLParser();
    private static final LLMNLParser llmParser = new LLMNLParser();

    enum Variant { STANDARD, COLLOQUIAL, ABBREVIATED, NOISY }

    record TestCase(String input, NLIntent expectedIntent, String expectedEntityType,
                    List<String> expectedEntityIds, Variant variant, String category) {}

    record Result(String category, String variant, String input,
                  String ruleIntent, String llmIntent, boolean ruleIntentOk, boolean llmIntentOk,
                  String ruleEntityType, String llmEntityType, boolean ruleTypeOk, boolean llmTypeOk,
                  String ruleEntityId, String llmEntityId, boolean ruleIdOk, boolean llmIdOk,
                  long llmLatencyMs) {}

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    @DisplayName("运行DeepSeek V4 Flash对比实验")
    void runExperiment() throws IOException {
        List<TestCase> cases = buildAllTestCases();
        List<Result> results = new ArrayList<>();

        for (TestCase tc : cases) {
            NLParser.ParseResult rule = ruleParser.parse(tc.input);
            long start = System.nanoTime();
            NLParser.ParseResult llm = llmParser.parse(tc.input);
            long llmLatMs = (System.nanoTime() - start) / 1_000_000;

            boolean ruleIntentOk = rule.getIntent() == tc.expectedIntent;
            boolean llmIntentOk = llm.getIntent() == tc.expectedIntent;
            boolean ruleTypeOk = Objects.equals(rule.getEntityType(), tc.expectedEntityType);
            boolean llmTypeOk = Objects.equals(llm.getEntityType(), tc.expectedEntityType);
            boolean ruleIdOk = idsEqual(rule.getEntityIds(), tc.expectedEntityIds);
            boolean llmIdOk = idsEqual(llm.getEntityIds(), tc.expectedEntityIds);

            results.add(new Result(
                    tc.category, tc.variant.name(), tc.input,
                    rule.getIntent() != null ? rule.getIntent().name() : "NULL",
                    llm.getIntent() != null ? llm.getIntent().name() : "NULL",
                    ruleIntentOk, llmIntentOk,
                    nvl(rule.getEntityType()), nvl(llm.getEntityType()),
                    ruleTypeOk, llmTypeOk,
                    rule.getEntityIds().toString(), llm.getEntityIds().toString(),
                    ruleIdOk, llmIdOk,
                    llmLatMs
            ));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("===== 实验四：DeepSeek V4 Flash vs 规则引擎基线对比 =====\n\n");

        // 汇总表
        csv.append("--- 4.1 规则引擎 vs DeepSeek 准确率对比 ---\n");
        csv.append("类别,样本数,规则意图(%),LLM意图(%),规则类型(%),LLM类型(%),规则ID(%),LLM ID(%),规则综合(%),LLM综合(%)\n");
        for (String cat : List.of("QUERY", "CREATE", "UPDATE", "DELETE", "STATISTICS")) {
            List<Result> cr = results.stream().filter(r -> r.category.equals(cat)).toList();
            if (cr.isEmpty()) continue;
            csv.append(String.format("%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f\n",
                    cat, cr.size(),
                    cr.stream().filter(r -> r.ruleIntentOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.llmIntentOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.ruleTypeOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.llmTypeOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.ruleIdOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.llmIdOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.ruleIntentOk && r.ruleTypeOk && r.ruleIdOk).count() * 100.0 / cr.size(),
                    cr.stream().filter(r -> r.llmIntentOk && r.llmTypeOk && r.llmIdOk).count() * 100.0 / cr.size()
            ));
        }
        List<Result> all = results;
        csv.append(String.format("总体,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f\n",
                all.size(),
                all.stream().filter(r -> r.ruleIntentOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.llmIntentOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.ruleTypeOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.llmTypeOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.ruleIdOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.llmIdOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.ruleIntentOk && r.ruleTypeOk && r.ruleIdOk).count() * 100.0 / all.size(),
                all.stream().filter(r -> r.llmIntentOk && r.llmTypeOk && r.llmIdOk).count() * 100.0 / all.size()
        ));

        // 变体对比表（核心对比表）
        csv.append("\n--- 4.2 变体准确率对比 (核心对比表) ---\n");
        csv.append("变体,规则意图(%),LLM意图(%),规则类型(%),LLM类型(%),规则ID(%),LLM ID(%),规则综合(%),LLM综合(%)\n");
        for (Variant v : Variant.values()) {
            List<Result> vr = results.stream().filter(r -> r.variant.equals(v.name())).toList();
            csv.append(String.format("%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f\n",
                    v.name(), vr.size(),
                    vr.stream().filter(r -> r.ruleIntentOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.llmIntentOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.ruleTypeOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.llmTypeOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.ruleIdOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.llmIdOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.ruleIntentOk && r.ruleTypeOk && r.ruleIdOk).count() * 100.0 / vr.size(),
                    vr.stream().filter(r -> r.llmIntentOk && r.llmTypeOk && r.llmIdOk).count() * 100.0 / vr.size()
            ));
        }

        // 延迟表
        csv.append("\n--- 4.3 延迟对比 ---\n");
        List<Long> llmLatencies = results.stream().map(r -> r.llmLatencyMs).sorted().toList();
        double avgLat = llmLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        double p50 = percentile(llmLatencies, 50);
        double p95 = percentile(llmLatencies, 95);
        double p99 = percentile(llmLatencies, 99);
        csv.append("方法,平均延迟(ms),P50(ms),P95(ms),P99(ms),最小(ms),最大(ms)\n");
        csv.append(String.format("Rule Engine,<1,<1,<1,<1,<1,<1\n"));
        csv.append(String.format("DeepSeek V4 Flash,%.1f,%.0f,%.0f,%.0f,%d,%d\n",
                avgLat, p50, p95, p99, llmLatencies.get(0), llmLatencies.get(llmLatencies.size() - 1)));

        // 逐条详情
        csv.append("\n--- 4.4 逐条测试结果 ---\n");
        csv.append("类别,变体,输入,规则意图,LLM意图,规则类型,LLM类型,规则ID,LLM ID,LLM延迟(ms)\n");
        for (Result r : results) {
            csv.append(String.format("%s,%s,\"%s\",%s,%s,%s,%s,%s,%s,%d\n",
                    r.category, r.variant, r.input,
                    r.ruleIntent, r.llmIntent,
                    r.ruleEntityType, r.llmEntityType,
                    r.ruleEntityId, r.llmEntityId,
                    r.llmLatencyMs));
        }

        Path outPath = Paths.get("experiment_results", "experiment4_llm_baseline.csv");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, csv.toString(), StandardCharsets.UTF_8);
        System.out.println("\n[实验四] 结果已写入: " + outPath.toAbsolutePath());
        System.out.println(csv);
    }

    // ==================== 100条测试用例 ====================

    private List<TestCase> buildAllTestCases() {
        List<TestCase> all = new ArrayList<>();
        all.addAll(buildQueryCases());
        all.addAll(buildCreateCases());
        all.addAll(buildUpdateCases());
        all.addAll(buildDeleteCases());
        all.addAll(buildStatisticsCases());
        return all;
    }

    private List<TestCase> buildQueryCases() {
        List<TestCase> list = new ArrayList<>();
        // 标准 (5)
        list.add(tc("查询设备EQ-2024-001的状态", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-001"), Variant.STANDARD, "QUERY"));
        list.add(tc("查询耗材CS-2024-002的库存", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-002"), Variant.STANDARD, "QUERY"));
        list.add(tc("查询巡检记录IN-2024-001", NLIntent.QUERY, "INSPECTION", List.of("IN-2024-001"), Variant.STANDARD, "QUERY"));
        list.add(tc("查询设备EQ-2024-005的维护记录", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-005"), Variant.STANDARD, "QUERY"));
        list.add(tc("查询耗材CS-2024-010的使用情况", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-010"), Variant.STANDARD, "QUERY"));
        // 口语化 (5)
        list.add(tc("帮我看看EQ-2024-001咋样了", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-001"), Variant.COLLOQUIAL, "QUERY"));
        list.add(tc("CS-2024-002还有多少库存", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-002"), Variant.COLLOQUIAL, "QUERY"));
        list.add(tc("查一下IN-2024-001的巡检情况", NLIntent.QUERY, "INSPECTION", List.of("IN-2024-001"), Variant.COLLOQUIAL, "QUERY"));
        list.add(tc("EQ-2024-005的设备状态是什么", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-005"), Variant.COLLOQUIAL, "QUERY"));
        list.add(tc("CS-2024-010用了多少了", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-010"), Variant.COLLOQUIAL, "QUERY"));
        // 简写 (5)
        list.add(tc("查询EQ-2024-001", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-001"), Variant.ABBREVIATED, "QUERY"));
        list.add(tc("CS-2024-002状态", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-002"), Variant.ABBREVIATED, "QUERY"));
        list.add(tc("IN-2024-001详情", NLIntent.QUERY, "INSPECTION", List.of("IN-2024-001"), Variant.ABBREVIATED, "QUERY"));
        list.add(tc("查询EQ-2024-005", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-005"), Variant.ABBREVIATED, "QUERY"));
        list.add(tc("获取CS-2024-010库存", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-010"), Variant.ABBREVIATED, "QUERY"));
        // 噪声 (5)
        list.add(tc("麻烦帮忙查询一下设备EQ-2024-001的当前状态信息怎么样", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-001"), Variant.NOISY, "QUERY"));
        list.add(tc("那个耗材CS-2024-002的库存还有多少帮我看看", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-002"), Variant.NOISY, "QUERY"));
        list.add(tc("就是那个巡检记录IN-2024-001查一下", NLIntent.QUERY, "INSPECTION", List.of("IN-2024-001"), Variant.NOISY, "QUERY"));
        list.add(tc("设备编号EQ-2024-005的维护情况麻烦查查", NLIntent.QUERY, "EQUIPMENT", List.of("EQ-2024-005"), Variant.NOISY, "QUERY"));
        list.add(tc("嗯关于耗材CS-2024-010的使用记录能查一下吗", NLIntent.QUERY, "CONSUMABLE", List.of("CS-2024-010"), Variant.NOISY, "QUERY"));
        return list;
    }

    private List<TestCase> buildCreateCases() {
        List<TestCase> list = new ArrayList<>();
        // 标准 (5)
        list.add(tc("新增耗材CS-2024-005", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-005"), Variant.STANDARD, "CREATE"));
        list.add(tc("新增设备EQ-2024-010", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-010"), Variant.STANDARD, "CREATE"));
        list.add(tc("创建巡检记录IN-2024-015", NLIntent.CREATE, "INSPECTION", List.of("IN-2024-015"), Variant.STANDARD, "CREATE"));
        list.add(tc("新增耗材CS-2024-020", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-020"), Variant.STANDARD, "CREATE"));
        list.add(tc("创建设备EQ-2024-030", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-030"), Variant.STANDARD, "CREATE"));
        // 口语化 (5)
        list.add(tc("加一个CS-2024-005的耗材", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-005"), Variant.COLLOQUIAL, "CREATE"));
        list.add(tc("新来了一台设备EQ-2024-010登记一下", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-010"), Variant.COLLOQUIAL, "CREATE"));
        list.add(tc("建一个巡检记录IN-2024-015", NLIntent.CREATE, "INSPECTION", List.of("IN-2024-015"), Variant.COLLOQUIAL, "CREATE"));
        list.add(tc("补一个耗材CS-2024-020的信息", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-020"), Variant.COLLOQUIAL, "CREATE"));
        list.add(tc("登记新设备EQ-2024-030", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-030"), Variant.COLLOQUIAL, "CREATE"));
        // 简写 (5)
        list.add(tc("新增CS-2024-005", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-005"), Variant.ABBREVIATED, "CREATE"));
        list.add(tc("添加EQ-2024-010", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-010"), Variant.ABBREVIATED, "CREATE"));
        list.add(tc("创建IN-2024-015", NLIntent.CREATE, "INSPECTION", List.of("IN-2024-015"), Variant.ABBREVIATED, "CREATE"));
        list.add(tc("新建CS-2024-020", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-020"), Variant.ABBREVIATED, "CREATE"));
        list.add(tc("添加EQ-2024-030", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-030"), Variant.ABBREVIATED, "CREATE"));
        // 噪声 (5)
        list.add(tc("我们需要新增一个耗材编号CS-2024-005麻烦登记一下", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-005"), Variant.NOISY, "CREATE"));
        list.add(tc("新到了一台设备EQ-2024-010请帮忙在系统里创建", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-010"), Variant.NOISY, "CREATE"));
        list.add(tc("巡检完成需要创建记录IN-2024-015记录一下", NLIntent.CREATE, "INSPECTION", List.of("IN-2024-015"), Variant.NOISY, "CREATE"));
        list.add(tc("麻烦把耗材CS-2024-020录入到系统里", NLIntent.CREATE, "CONSUMABLE", List.of("CS-2024-020"), Variant.NOISY, "CREATE"));
        list.add(tc("新设备EQ-2024-030需要创建档案", NLIntent.CREATE, "EQUIPMENT", List.of("EQ-2024-030"), Variant.NOISY, "CREATE"));
        return list;
    }

    private List<TestCase> buildUpdateCases() {
        List<TestCase> list = new ArrayList<>();
        // 标准 (5)
        list.add(tc("修改设备EQ-2024-003的维护周期", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-003"), Variant.STANDARD, "UPDATE"));
        list.add(tc("更新耗材CS-2024-008的库存数量", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-008"), Variant.STANDARD, "UPDATE"));
        list.add(tc("修改巡检记录IN-2024-012的状态", NLIntent.UPDATE, "INSPECTION", List.of("IN-2024-012"), Variant.STANDARD, "UPDATE"));
        list.add(tc("更新设备EQ-2024-025的使用状态", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-025"), Variant.STANDARD, "UPDATE"));
        list.add(tc("修改耗材CS-2024-015的规格参数", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-015"), Variant.STANDARD, "UPDATE"));
        // 口语化 (5)
        list.add(tc("把EQ-2024-003的维护周期改一下", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-003"), Variant.COLLOQUIAL, "UPDATE"));
        list.add(tc("CS-2024-008库存数据要更新了", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-008"), Variant.COLLOQUIAL, "UPDATE"));
        list.add(tc("IN-2024-012的记录状态改一下", NLIntent.UPDATE, "INSPECTION", List.of("IN-2024-012"), Variant.COLLOQUIAL, "UPDATE"));
        list.add(tc("EQ-2024-025这设备不是闲置了变更状态", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-025"), Variant.COLLOQUIAL, "UPDATE"));
        list.add(tc("CS-2024-015的规格变了", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-015"), Variant.COLLOQUIAL, "UPDATE"));
        // 简写 (5)
        list.add(tc("修改EQ-2024-003周期", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-003"), Variant.ABBREVIATED, "UPDATE"));
        list.add(tc("更新CS-2024-008库存", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-008"), Variant.ABBREVIATED, "UPDATE"));
        list.add(tc("修改IN-2024-012", NLIntent.UPDATE, "INSPECTION", List.of("IN-2024-012"), Variant.ABBREVIATED, "UPDATE"));
        list.add(tc("更新EQ-2024-025状态", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-025"), Variant.ABBREVIATED, "UPDATE"));
        list.add(tc("编辑CS-2024-015参数", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-015"), Variant.ABBREVIATED, "UPDATE"));
        // 噪声 (5)
        list.add(tc("需要把设备EQ-2024-003的维护周期调整一下改为每月一次", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-003"), Variant.NOISY, "UPDATE"));
        list.add(tc("耗材CS-2024-008的库存数量好像不对需要更新一下", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-008"), Variant.NOISY, "UPDATE"));
        list.add(tc("巡检记录IN-2024-012的状态需要修改为已完成", NLIntent.UPDATE, "INSPECTION", List.of("IN-2024-012"), Variant.NOISY, "UPDATE"));
        list.add(tc("那个设备EQ-2024-025的状态麻烦更新一下改成维修中", NLIntent.UPDATE, "EQUIPMENT", List.of("EQ-2024-025"), Variant.NOISY, "UPDATE"));
        list.add(tc("耗材编号CS-2024-015的规格参数需要更新一下谢谢", NLIntent.UPDATE, "CONSUMABLE", List.of("CS-2024-015"), Variant.NOISY, "UPDATE"));
        return list;
    }

    private List<TestCase> buildDeleteCases() {
        List<TestCase> list = new ArrayList<>();
        // 标准 (5)
        list.add(tc("删除巡检记录IN-2024-010", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-010"), Variant.STANDARD, "DELETE"));
        list.add(tc("删除耗材CS-2024-099", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-099"), Variant.STANDARD, "DELETE"));
        list.add(tc("删除设备EQ-2024-088", NLIntent.DELETE, "EQUIPMENT", List.of("EQ-2024-088"), Variant.STANDARD, "DELETE"));
        list.add(tc("删除巡检记录IN-2024-050", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-050"), Variant.STANDARD, "DELETE"));
        list.add(tc("删除耗材CS-2024-077", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-077"), Variant.STANDARD, "DELETE"));
        // 口语化 (5)
        list.add(tc("把IN-2024-010这条记录删了", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-010"), Variant.COLLOQUIAL, "DELETE"));
        list.add(tc("CS-2024-099不需要了删掉", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-099"), Variant.COLLOQUIAL, "DELETE"));
        list.add(tc("EQ-2024-088报废了移除", NLIntent.DELETE, "EQUIPMENT", List.of("EQ-2024-088"), Variant.COLLOQUIAL, "DELETE"));
        list.add(tc("IN-2024-050这条巡检记录作废", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-050"), Variant.COLLOQUIAL, "DELETE"));
        list.add(tc("CS-2024-077这个耗材登记错了删除", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-077"), Variant.COLLOQUIAL, "DELETE"));
        // 简写 (5)
        list.add(tc("删除IN-2024-010", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-010"), Variant.ABBREVIATED, "DELETE"));
        list.add(tc("移除CS-2024-099", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-099"), Variant.ABBREVIATED, "DELETE"));
        list.add(tc("删除EQ-2024-088", NLIntent.DELETE, "EQUIPMENT", List.of("EQ-2024-088"), Variant.ABBREVIATED, "DELETE"));
        list.add(tc("删除IN-2024-050", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-050"), Variant.ABBREVIATED, "DELETE"));
        list.add(tc("移除CS-2024-077", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-077"), Variant.ABBREVIATED, "DELETE"));
        // 噪声 (5)
        list.add(tc("巡检记录IN-2024-010数据有误需要删除重新录入", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-010"), Variant.NOISY, "DELETE"));
        list.add(tc("耗材CS-2024-099已经过期了请帮忙删除掉", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-099"), Variant.NOISY, "DELETE"));
        list.add(tc("设备EQ-2024-088已经报废了麻烦移除相关记录", NLIntent.DELETE, "EQUIPMENT", List.of("EQ-2024-088"), Variant.NOISY, "DELETE"));
        list.add(tc("那个旧的巡检记录IN-2024-050没有用了可以删掉", NLIntent.DELETE, "INSPECTION", List.of("IN-2024-050"), Variant.NOISY, "DELETE"));
        list.add(tc("不好意思CS-2024-077录入重复了请删除", NLIntent.DELETE, "CONSUMABLE", List.of("CS-2024-077"), Variant.NOISY, "DELETE"));
        return list;
    }

    private List<TestCase> buildStatisticsCases() {
        List<TestCase> list = new ArrayList<>();
        // 标准 (5)
        list.add(tc("统计本月耗材消耗数量", NLIntent.STATISTICS, "CONSUMABLE", List.of(), Variant.STANDARD, "STATISTICS"));
        list.add(tc("统计本季度设备故障次数", NLIntent.STATISTICS, "EQUIPMENT", List.of(), Variant.STANDARD, "STATISTICS"));
        list.add(tc("统计本月巡检完成率", NLIntent.STATISTICS, "INSPECTION", List.of(), Variant.STANDARD, "STATISTICS"));
        list.add(tc("统计耗材CS-2024-001的总消耗", NLIntent.STATISTICS, "CONSUMABLE", List.of("CS-2024-001"), Variant.STANDARD, "STATISTICS"));
        list.add(tc("统计设备EQ-2024-005的巡检次数", NLIntent.STATISTICS, "EQUIPMENT", List.of("EQ-2024-005"), Variant.STANDARD, "STATISTICS"));
        // 口语化 (5)
        list.add(tc("帮我看看这个月用了多少耗材", NLIntent.STATISTICS, "CONSUMABLE", List.of(), Variant.COLLOQUIAL, "STATISTICS"));
        list.add(tc("这季度设备坏了多少次统计一下", NLIntent.STATISTICS, "EQUIPMENT", List.of(), Variant.COLLOQUIAL, "STATISTICS"));
        list.add(tc("本月巡检完成了多少统计一下", NLIntent.STATISTICS, "INSPECTION", List.of(), Variant.COLLOQUIAL, "STATISTICS"));
        list.add(tc("CS-2024-001一共用了多少", NLIntent.STATISTICS, "CONSUMABLE", List.of("CS-2024-001"), Variant.COLLOQUIAL, "STATISTICS"));
        list.add(tc("EQ-2024-005巡检过几次", NLIntent.STATISTICS, "EQUIPMENT", List.of("EQ-2024-005"), Variant.COLLOQUIAL, "STATISTICS"));
        // 简写 (5)
        list.add(tc("统计本月耗材", NLIntent.STATISTICS, "CONSUMABLE", List.of(), Variant.ABBREVIATED, "STATISTICS"));
        list.add(tc("汇总设备故障", NLIntent.STATISTICS, "EQUIPMENT", List.of(), Variant.ABBREVIATED, "STATISTICS"));
        list.add(tc("统计巡检完成", NLIntent.STATISTICS, "INSPECTION", List.of(), Variant.ABBREVIATED, "STATISTICS"));
        list.add(tc("耗材消耗数量统计", NLIntent.STATISTICS, "CONSUMABLE", List.of(), Variant.ABBREVIATED, "STATISTICS"));
        list.add(tc("设备巡检统计", NLIntent.STATISTICS, "EQUIPMENT", List.of(), Variant.ABBREVIATED, "STATISTICS"));
        // 噪声 (5)
        list.add(tc("麻烦帮我统计一下这个月大概消耗了多少耗材", NLIntent.STATISTICS, "CONSUMABLE", List.of(), Variant.NOISY, "STATISTICS"));
        list.add(tc("我想了解一下本季度设备故障的次数大概是多少", NLIntent.STATISTICS, "EQUIPMENT", List.of(), Variant.NOISY, "STATISTICS"));
        list.add(tc("帮我汇总一下本月巡检记录的完成情况吧", NLIntent.STATISTICS, "INSPECTION", List.of(), Variant.NOISY, "STATISTICS"));
        list.add(tc("查询一下耗材CS-2024-001总的消耗情况统计一下", NLIntent.STATISTICS, "CONSUMABLE", List.of("CS-2024-001"), Variant.NOISY, "STATISTICS"));
        list.add(tc("统计一下设备EQ-2024-005历史巡检的次数有多少", NLIntent.STATISTICS, "EQUIPMENT", List.of("EQ-2024-005"), Variant.NOISY, "STATISTICS"));
        return list;
    }

    private TestCase tc(String input, NLIntent intent, String entityType,
                        List<String> entityIds, Variant variant, String category) {
        return new TestCase(input, intent, entityType, entityIds, variant, category);
    }

    private boolean idsEqual(List<String> a, List<String> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        List<String> sa = new ArrayList<>(a); Collections.sort(sa);
        List<String> sb = new ArrayList<>(b); Collections.sort(sb);
        return sa.equals(sb);
    }

    private String nvl(String s) { return s != null ? s : "NULL"; }

    private double percentile(List<Long> sorted, double pct) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}
