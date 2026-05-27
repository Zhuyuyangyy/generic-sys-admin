package com.zyy.nl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * DeepSeek V4 Flash NL解析器。
 * <p>调用LLM API识别意图和实体，与规则引擎NLParser接口兼容。</p>
 */
@Component
public class LLMNLParser {

    private static final String BASE_URL = "https://api.deepseek.com";
    private static final String MODEL = "deepseek-chat";

    private static final String SYSTEM_PROMPT = """
            You are an enterprise resource management system parser.
            Extract intent and entity from the user instruction.
            Reply ONLY in JSON format:
            {"intent":"QUERY|CREATE|UPDATE|DELETE|STATISTICS",
             "entityType":"EQUIPMENT|CONSUMABLE|INSPECTION|NONE",
             "entityId":"extracted ID or null"}
            """;

    private final HttpClient httpClient;
    private final String apiKey;

    public LLMNLParser() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key == null || key.isBlank()) {
            this.apiKey = null;
            System.out.println("[LLMNLParser] DEEPSEEK_API_KEY not set, LLM fallback disabled");
        } else {
            this.apiKey = key;
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public NLParser.ParseResult parse(String input) {
        if (apiKey == null) {
            throw new IllegalStateException("DEEPSEEK_API_KEY not configured");
        }
        if (input == null || input.isBlank()) {
            return new NLParser.ParseResult(null, java.util.List.of(), null);
        }

        try {
            String jsonBody = buildRequestBody(input);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("DeepSeek API error " + response.statusCode() + ": " + response.body());
                return new NLParser.ParseResult(null, java.util.List.of(), null);
            }

            return parseResponse(response.body(), input);
        } catch (Exception e) {
            System.err.println("LLM parse failed for: " + input + " | " + e.getMessage());
            return new NLParser.ParseResult(null, java.util.List.of(), null);
        }
    }

    private String buildRequestBody(String userInput) {
        JSONObject body = new JSONObject();
        body.set("model", MODEL);
        body.set("temperature", 0.0);
        body.set("max_tokens", 64);

        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", SYSTEM_PROMPT.trim());

        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userInput);

        body.set("messages", java.util.List.of(systemMsg, userMsg));
        return body.toString();
    }

    private NLParser.ParseResult parseResponse(String responseBody, String input) {
        try {
            JSONObject root = JSONUtil.parseObj(responseBody);
            JSONObject choice = JSONUtil.parseObj(
                    JSONUtil.parseArray(root.getStr("choices")).get(0));
            String content = JSONUtil.parseObj(choice.getStr("message")).getStr("content").trim();

            if (content.startsWith("```")) {
                int firstBrace = content.indexOf('{');
                int lastBrace = content.lastIndexOf('}');
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    content = content.substring(firstBrace, lastBrace + 1);
                }
            }

            JSONObject parsed = JSONUtil.parseObj(content);

            String intentStr = parsed.getStr("intent", "QUERY");
            NLIntent intent;
            try {
                intent = NLIntent.valueOf(intentStr);
            } catch (IllegalArgumentException e) {
                intent = NLIntent.QUERY;
            }

            String entityTypeStr = parsed.getStr("entityType", "NONE");
            String entityType = "NONE".equals(entityTypeStr) ? null : entityTypeStr;

            String entityId = parsed.getStr("entityId");
            java.util.List<String> entityIds = java.util.List.of();
            if (entityId != null && !entityId.equalsIgnoreCase("null") && !entityId.isBlank()) {
                entityIds = java.util.List.of(entityId);
            }

            System.out.println("[LLM] parsed -> intent=" + intent + " type=" + entityType + " ids=" + entityIds);
            return new NLParser.ParseResult(intent, entityIds, entityType);
        } catch (Exception e) {
            System.err.println("Failed to parse LLM response: " + e.getMessage() + " | body: " + responseBody);
            return new NLParser.ParseResult(NLIntent.QUERY, java.util.List.of(), null);
        }
    }
}
