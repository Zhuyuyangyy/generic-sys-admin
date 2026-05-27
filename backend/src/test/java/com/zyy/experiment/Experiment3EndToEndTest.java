package com.zyy.experiment;

import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.redis.enabled=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.sql.init.mode=never",
        "mybatis-plus.mapper-locations=",
        "storage.provider=local",
        "minio.endpoint=localhost:99999",
        "minio.bucket-name=test",
        "minio.access-key=test",
        "minio.secret-key=test"
    }
)
@Import(Experiment3EndToEndTest.TestNLSecurityConfig.class)
@DisplayName("实验三：端到端响应时间测试")
public class Experiment3EndToEndTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private MinioClient minioClient;

    private static final List<String> CAUSAL_INPUTS = List.of(
        "修改设备EQ-2024-003的维护周期",
        "修改巡检记录IN-2024-012的状态",
        "删除巡检记录IN-2024-010",
        "删除耗材CS-2024-077",
        "更新设备EQ-2024-025的使用状态",
        "修改耗材CS-2024-015的规格参数",
        "删除设备EQ-2024-088",
        "更新耗材CS-2024-008的库存数量"
    );

    private static final List<String> QUERY_INPUTS = List.of(
        "查询设备EQ-2024-001的状态",
        "查询耗材CS-2024-002的库存",
        "查询巡检记录IN-2024-001",
        "获取设备EQ-2024-005的维护记录",
        "查询耗材CS-2024-010的使用情况",
        "获取巡检记录IN-2024-050",
        "查询设备EQ-2024-003的状态",
        "查询耗材CS-2024-020的库存"
    );

    @TestConfiguration
    @Order(-100)
    static class TestNLSecurityConfig {
        @Bean
        public SecurityFilterChain nlSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/api/nl/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @DisplayName("运行端到端响应时间实验")
    void runExperiment() throws Exception {
        String baseUrl = "http://localhost:" + port;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        StringBuilder csv = new StringBuilder();
        csv.append("===== 实验三：端到端响应时间 =====\n\n");

        // ---- 公平对比: 预热 + 交替单用户 + 独立并发对比 ----
        csv.append("--- 3.1~3.6 公平测试设计 ---\n");
        csv.append("测试策略: Warmup(各10次) → 单用户交替(100对) → 并发独立公平跑\n\n");

        // 预热: 两组交替各10次，消除JIT差异
        csv.append("=== Warmup (因果检查 + 对照组 各10次交替) ===\n");
        for (int i = 0; i < 10; i++) {
            sendRequest(client, baseUrl, CAUSAL_INPUTS.get(i % CAUSAL_INPUTS.size()), true);
            sendRequest(client, baseUrl, QUERY_INPUTS.get(i % QUERY_INPUTS.size()), false);
        }

        // 单用户: 严格交替 100对（1因果 + 1对照 = 1对），消除顺序偏差
        csv.append("\n=== 单用户 100对交替测试 ===\n");
        List<Long> causalSingle = new ArrayList<>();
        List<Long> ctrlSingle = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            causalSingle.add(sendRequest(client, baseUrl, CAUSAL_INPUTS.get(i % CAUSAL_INPUTS.size()), true));
            ctrlSingle.add(sendRequest(client, baseUrl, QUERY_INPUTS.get(i % QUERY_INPUTS.size()), false));
        }
        appendStats(csv, causalSingle, "因果检查-单用户");
        appendStats(csv, ctrlSingle, "对照组-单用户");

        // 并发: 两组严格交替并发（每对请求同时出发，对称竞争）
        csv.append("\n=== 10并发 严格交替并发测试 ===\n");
        List<long[]> c10 = runConcurrentPaired(baseUrl, 10, 100);
        List<Long> causalC10 = new ArrayList<>();
        List<Long> ctrlC10 = new ArrayList<>();
        for (long[] p : c10) { causalC10.add(p[0]); ctrlC10.add(p[1]); }
        appendStats(csv, causalC10, "因果检查-10并发");
        appendStats(csv, ctrlC10, "对照组-10并发");

        csv.append("\n=== 50并发 严格交替并发测试 ===\n");
        List<long[]> c50 = runConcurrentPaired(baseUrl, 50, 100);
        List<Long> causalC50 = new ArrayList<>();
        List<Long> ctrlC50 = new ArrayList<>();
        for (long[] p : c50) { causalC50.add(p[0]); ctrlC50.add(p[1]); }
        appendStats(csv, causalC50, "因果检查-50并发");
        appendStats(csv, ctrlC50, "对照组-50并发");

        // ---- 汇总对比表 ----
        csv.append("\n--- 3.7 多场景性能对比 (因果检查组 vs 对照组) ---\n");
        csv.append("场景,接口,样本数,平均响应时间(ms),P95(ms),P99(ms),吞吐量(req/s)\n");
        appendSummaryRow(csv, "单用户", "因果检查", causalSingle);
        appendSummaryRow(csv, "单用户", "对照组", ctrlSingle);
        appendSummaryRow(csv, "10并发", "因果检查", causalC10);
        appendSummaryRow(csv, "10并发", "对照组", ctrlC10);
        appendSummaryRow(csv, "50并发", "因果检查", causalC50);
        appendSummaryRow(csv, "50并发", "对照组", ctrlC50);

        // ---- 因果检查额外开销 ----
        csv.append("\n--- 3.8 因果检查额外开销分析 ---\n");
        csv.append("场景,因果检查平均(ms),对照组平均(ms),额外开销(ms),开销比例(%)\n");
        appendOverheadRow(csv, "单用户", causalSingle, ctrlSingle);
        appendOverheadRow(csv, "10并发", causalC10, ctrlC10);
        appendOverheadRow(csv, "50并发", causalC50, ctrlC50);

        Path outPath = Paths.get("experiment_results", "experiment3_e2e_response_time.csv");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, csv.toString(), StandardCharsets.UTF_8);
        System.out.println("\n[实验三] 结果已写入: " + outPath.toAbsolutePath());
        System.out.println(csv);
    }

    private long sendRequest(HttpClient client, String baseUrl, String input, boolean withCausalCheck) throws Exception {
        String endpoint = withCausalCheck ? "/api/nl/execute-with-causal-check" : "/api/nl/execute";
        String json = "{\"input\":\"" + input + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();
        long start = System.nanoTime();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        if (resp.statusCode() != 200) {
            System.err.println("WARN: HTTP " + resp.statusCode() + " for " + endpoint + " input: " + input);
        }
        return elapsedMs;
    }

    private List<Long> runConcurrent(HttpClient client, String baseUrl, int concurrency, int requestsPerThread,
                                     boolean withCausalCheck) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<List<Long>>> futures = new ArrayList<>();
        List<String> inputs = withCausalCheck ? CAUSAL_INPUTS : QUERY_INPUTS;

        for (int t = 0; t < concurrency; t++) {
            final int threadIdx = t;
            futures.add(executor.submit(() -> {
                List<Long> latencies = new ArrayList<>();
                for (int i = 0; i < requestsPerThread; i++) {
                    String input = inputs.get((threadIdx * requestsPerThread + i) % inputs.size());
                    try {
                        latencies.add(sendRequest(client, baseUrl, input, withCausalCheck));
                    } catch (Exception e) {
                        System.err.println("Thread " + threadIdx + " request " + i + " failed: " + e.getMessage());
                        latencies.add(-1L);
                    }
                }
                return latencies;
            }));
        }

        List<Long> allLatencies = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            allLatencies.addAll(future.get(300, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return allLatencies;
    }

    private List<long[]> runConcurrentPaired(String baseUrl, int concurrency, int rounds) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency * 2);
        List<Future<long[]>> futures = new ArrayList<>();

        for (int r = 0; r < rounds; r++) {
            final int round = r;
            for (int t = 0; t < concurrency; t++) {
                final int threadIdx = t;
                futures.add(executor.submit(() -> {
                    String causalInput = CAUSAL_INPUTS.get((round * concurrency + threadIdx) % CAUSAL_INPUTS.size());
                    String ctrlInput = QUERY_INPUTS.get((round * concurrency + threadIdx) % QUERY_INPUTS.size());
                    long causalLat = sendRequest(client, baseUrl, causalInput, true);
                    long ctrlLat = sendRequest(client, baseUrl, ctrlInput, false);
                    return new long[]{causalLat, ctrlLat};
                }));
            }
        }

        List<long[]> results = new ArrayList<>();
        for (Future<long[]> f : futures) {
            results.add(f.get(60, TimeUnit.SECONDS));
        }
        executor.shutdown();
        return results;
    }

    private void appendStats(StringBuilder csv, List<Long> latencies, String scenario) {
        List<Long> valid = latencies.stream().filter(l -> l >= 0).sorted().toList();
        if (valid.isEmpty()) {
            csv.append(scenario).append(": 无有效数据\n");
            return;
        }
        double avg = valid.stream().mapToLong(Long::longValue).average().orElse(0);
        double p95 = percentile(valid, 95);
        double p99 = percentile(valid, 99);
        double min = valid.get(0);
        double max = valid.get(valid.size() - 1);
        csv.append(String.format("场景: %s\n", scenario));
        csv.append(String.format("  样本数: %d\n", valid.size()));
        csv.append(String.format("  平均(ms): %.2f\n", avg));
        csv.append(String.format("  Min(ms): %.0f\n", min));
        csv.append(String.format("  Max(ms): %.0f\n", max));
        csv.append(String.format("  P95(ms): %.0f\n", p95));
        csv.append(String.format("  P99(ms): %.0f\n", p99));
    }

    private void appendSummaryRow(StringBuilder csv, String scenario, String group, List<Long> latencies) {
        List<Long> valid = latencies.stream().filter(l -> l >= 0).sorted().toList();
        if (valid.isEmpty()) {
            csv.append(String.format("%s,%s,0,N/A,N/A,N/A,N/A\n", scenario, group));
            return;
        }
        double avg = valid.stream().mapToLong(Long::longValue).average().orElse(0);
        double p95 = percentile(valid, 95);
        double p99 = percentile(valid, 99);
        double totalSec = valid.stream().mapToLong(Long::longValue).sum() / 1000.0;
        double throughput = totalSec > 0 ? valid.size() / totalSec : 0;
        csv.append(String.format("%s,%s,%d,%.2f,%.0f,%.0f,%.2f\n",
                scenario, group, valid.size(), avg, p95, p99, throughput));
    }

    private void appendOverheadRow(StringBuilder csv, String scenario, List<Long> causal, List<Long> control) {
        double causalAvg = causal.stream().filter(l -> l >= 0).mapToLong(Long::longValue).average().orElse(0);
        double ctrlAvg = control.stream().filter(l -> l >= 0).mapToLong(Long::longValue).average().orElse(0);
        double overhead = causalAvg - ctrlAvg;
        double pct = ctrlAvg > 0 ? (overhead / ctrlAvg) * 100 : 0;
        csv.append(String.format("%s,%.2f,%.2f,%.2f,%.1f\n", scenario, causalAvg, ctrlAvg, overhead, pct));
    }

    private double percentile(List<Long> sorted, double pct) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}