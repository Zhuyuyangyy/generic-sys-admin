package com.zyy.experiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
@Import(Experiment5CaseStudyTest.TestNLSecurityConfig.class)
@DisplayName("实验五：案例研究——因果感知NL执行演示")
public class Experiment5CaseStudyTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private MinioClient minioClient;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<Map<String, String>> SCENARIOS = List.of(
        Map.of("name", "S1", "input", "删除设备EQ-2024-001"),
        Map.of("name", "S2", "input", "修改耗材CS-2024-008的库存数量"),
        Map.of("name", "S3", "input", "查询设备EQ-2024-001的状态")
    );

    @TestConfiguration
    static class TestNLSecurityConfig {
        @Bean
        @Order(1)
        public SecurityFilterChain nlSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/api/nl/**")
                .csrf(c -> c.disable())
                .sessionManagement(s ->
                    s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @Order(1)
    @DisplayName("运行案例研究并生成报告")
    void runCaseStudyAndGenerateReport() throws Exception {
        String baseUrl = "http://localhost:" + port;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        List<String[]> rows = new ArrayList<>();

        for (Map<String, String> scenario : SCENARIOS) {
            String name = scenario.get("name");
            String input = scenario.get("input");

            String json = "{\"input\":\"" + input + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/nl/execute-with-causal-check"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            long start = System.nanoTime();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.println("=== " + name + ": " + input + " ===");
            System.out.println("Response (" + elapsedMs + "ms): " + resp.body());

            JsonNode root = mapper.readTree(resp.body());
            JsonNode data = root.path("data");

            String intent = data.has("causalCheckPerformed") && data.path("causalCheckPerformed").asBoolean()
                    ? extractIntentFromExecution(data.path("executionResult").asText())
                    : "UNKNOWN";

            String entityId = data.path("entityId").isNull() ? "" : data.path("entityId").asText();
            boolean causalCheckPerformed = data.path("causalCheckPerformed").asBoolean(false);

            int impactedCount = 0;
            JsonNode impactedNodes = data.path("impactedNodes");
            if (impactedNodes.isObject()) {
                impactedCount = impactedNodes.size();
            }

            boolean hasHighImpact = false;
            if (impactedNodes.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = impactedNodes.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    if (entry.getValue().asDouble(0) >= 0.5) {
                        hasHighImpact = true;
                        break;
                    }
                }
            }

            rows.add(new String[]{
                name, input, intent, entityId,
                String.valueOf(causalCheckPerformed),
                String.valueOf(impactedCount),
                String.valueOf(hasHighImpact),
                String.valueOf(elapsedMs)
            });
        }

        StringBuilder csv = new StringBuilder();
        csv.append("场景,输入,意图,实体ID,触发因果检查,影响节点数,hasHighImpact,响应时间(ms)\n");
        for (String[] row : rows) {
            csv.append(String.join(",", row)).append("\n");
        }

        Path outPath = Paths.get("experiment_results", "experiment5_case_study.csv");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, csv.toString(), StandardCharsets.UTF_8);
        System.out.println("\n[实验五] 结果已写入: " + outPath.toAbsolutePath());
        System.out.println(csv);
    }

    /** 从 executionResult 文本中粗略提取意图类型 */
    private String extractIntentFromExecution(String executionResult) {
        if (executionResult == null) return "UNKNOWN";
        if (executionResult.contains("删除") || executionResult.contains("delete")) return "DELETE";
        if (executionResult.contains("修改") || executionResult.contains("更新")
                || executionResult.contains("update")) return "UPDATE";
        if (executionResult.contains("查询") || executionResult.contains("获取")
                || executionResult.contains("query")) return "QUERY";
        if (executionResult.contains("新增") || executionResult.contains("创建")
                || executionResult.contains("create")) return "CREATE";
        if (executionResult.contains("统计") || executionResult.contains("汇总")
                || executionResult.contains("statistic")) return "STATISTICS";
        return "UNKNOWN";
    }
}
