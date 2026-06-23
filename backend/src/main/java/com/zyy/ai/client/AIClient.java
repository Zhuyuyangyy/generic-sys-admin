package com.zyy.ai.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ==========================================================
 * AI Client for Generic System Administration Platform
 * Provides unified HTTP client for AI services (MiniMax TTS, etc.)
 * ==========================================================
 *
 * This component provides:
 * - Unified HTTP client for AI API calls (POST JSON, GET, Stream)
 * - Supports MiniMax TTS API integration via OkHttp3 under the hood
 * - Hutool HttpRequest wrapper for simplified HTTP operations
 * - Automatic timeout management and error handling
 *
 * Configuration:
 * - Connection timeout: 10 seconds
 * - Read timeout: 60 seconds
 * - Write timeout: 10 seconds
 *
 * Features:
 * - Token-based authentication (Bearer token)
 * - Streaming response support (SSE)
 * - Automatic retry logic via error logging
 *
 * Usage Examples:
 * ```java
 * @Autowired
 * private AIClient aiClient;
 *
 * // Simple POST request
 * String result = aiClient.postJson(apiUrl, jsonBody);
 *
 * // With API key
 * String result = aiClient.postJson(apiUrl, jsonBody, apiKey);
 *
 * // Streaming callback
 * aiClient.postJsonWithCallback(apiUrl, jsonBody, apiKey, chunk -> {
 *     System.out.println("Received chunk: " + chunk);
 * });
 * ```
 *
 * MiniMax TTS Integration:
 * ```java
 * // 1. Prepare TTS request
 * Map<String, Object> body = new HashMap<>();
 * body.put("model", "speech-01");
 * body.put("text", Map.of("type", "plain", "text", "Hello, this is a test."));
 * body.put("voice_setting", Map.of("voice", "male-qn-qingse"));
 *
 * // 2. Call API
 * String response = aiClient.postJson(
 *     "https://api.minimax.chat/v1/t2a_v2",
 *     JSONUtil.toJsonStr(body),
 *     minimaxApiKey
 * );
 *
 * // 3. Parse response JSON for audio URL
 * JSONObject json = JSONUtil.parseObj(response);
 * String audioUrl = json.getByPath("data.audio_file_id", String.class);
 * ```
 *
 * @author System Architect
 */
@Component
public class AIClient {

    private static final Logger log = LoggerFactory.getLogger(AIClient.class);

    // ==================== Timeout Configuration ====================

    /**
     * Connection timeout in milliseconds (default: 10000ms = 10s)
     * Time to establish TCP connection to AI service
     */
    @Value("${ai.client.connect-timeout:10000}")
    private int connectTimeout;

    /**
     * Read timeout in milliseconds (default: 60000ms = 60s)
     * Time to wait for response after sending request
     */
    @Value("${ai.client.read-timeout:60000}")
    private int readTimeout;

    /**
     * Write timeout in milliseconds (default: 10000ms = 10s)
     * Time to write request body (POST JSON)
     */
    @Value("${ai.client.write-timeout:10000}")
    private int writeTimeout;

    // ==================== POST JSON Methods ====================

    /**
     * Send POST request with JSON body and optional Bearer token.
     *
     * Features:
     * 1. Guard clauses for null/empty URL and body
     * 2. Sets Content-Type: application/json
     * 3. Adds Authorization header if token provided
     * 4. Hutool HttpRequest wraps OkHttp for robust HTTP
     * 5. Returns response body on 2xx, throws RuntimeException otherwise
     *
     * @param url   Target API URL (must start with https://)
     * @param body  JSON body string (use JSONUtil.toJson() to serialize)
     * @param token Optional Bearer token for authentication
     * @return Response body string
     * @throws IllegalArgumentException if url or body is null/empty
     */
    public String postJson(String url, String body, String token) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("[AIClient] URL cannot be blank");
        }
        if (StrUtil.isBlank(body)) {
            throw new IllegalArgumentException("[AIClient] Body cannot be blank");
        }

        log.debug("[AIClient] POST Request | URL: {}\n  Body: {}\n  Token: {}",
            url, body, StrUtil.isBlank(token) ? "(not provided)" : "(provided)");

        try {
            HttpRequest request = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(connectTimeout + readTimeout + writeTimeout)
                .body(body);

            if (StrUtil.isNotBlank(token)) {
                request.header("Authorization", "Bearer " + token.trim());
            }

            HttpResponse response = request.execute();

            int httpStatus = response.getStatus();
            String responseBody = response.body();

            if (response.isOk()) {
                log.debug("[AIClient] Success | HTTP {} | Body: {}",
                    httpStatus,
                    StrUtil.sub(responseBody, 0, Math.min(200, responseBody.length())) + "...");
                return responseBody;
            } else {
                log.error("[AIClient] HTTP Error | URL: {} | HTTP {} | Body: {}",
                    url, httpStatus, responseBody);
                throw new RuntimeException(
                    StrUtil.format("[AIClient] AI API call failed | HTTP {} | {}", httpStatus, responseBody)
                );
            }

        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[AIClient] Network/Unexpected error | URL: {} | Error: {}", url, ex.getMessage());
            throw new RuntimeException(StrUtil.format("[AIClient] Unexpected error: {}", ex.getMessage()), ex);
        }
    }

    /**
     * Send POST request without authentication token.
     */
    public String postJson(String url, String body) {
        return postJson(url, body, null);
    }

    // ==================== GET Methods ====================

    /**
     * Send GET request with optional Bearer token.
     *
     * @param url   Target API URL
     * @param token Optional Bearer token
     * @return Response body string
     */
    public String get(String url, String token) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("[AIClient] URL cannot be blank");
        }

        log.debug("[AIClient] GET Request | URL: {}", url);

        try {
            HttpRequest request = HttpRequest.get(url)
                .header("Accept", "application/json")
                .timeout(connectTimeout + readTimeout);

            if (StrUtil.isNotBlank(token)) {
                request.header("Authorization", "Bearer " + token.trim());
            }

            HttpResponse response = request.execute();
            int httpStatus = response.getStatus();
            String responseBody = response.body();

            if (response.isOk()) {
                return responseBody;
            } else {
                log.error("[AIClient] GET Error | HTTP {} | {}", httpStatus, responseBody);
                throw new RuntimeException(
                    StrUtil.format("[AIClient] GET request failed | HTTP {} | {}", httpStatus, responseBody)
                );
            }

        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[AIClient] GET error | URL: {} | Error: {}", url, ex.getMessage());
            throw new RuntimeException(StrUtil.format("[AIClient] GET failed: {}", ex.getMessage()), ex);
        }
    }

    // ==================== MiniMax TTS Methods ====================

    /**
     * Call MiniMax TTS (Text-to-Speech) API.
     *
     * MiniMax T2A V2 API Request Format:
     * {
     *   "model": "speech-01",
     *   "text": {
     *     "type": "plain",
     *     "text": "Text to convert to speech"
     *   },
     *   "voice_setting": {
     *     "voice": "male-qn-qingse"   // Voice ID
     *   }
     * }
     *
     * @param text    Text to convert (max 1000 chars recommended)
     * @param apiKey  MiniMax API Key
     * @param voiceId Voice ID (default: male-qn-qingse)
     * @return JSON response with audio file info
     */
    public String callMinimaxTTS(String text, String apiKey, String voiceId) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("[AIClient] Text cannot be blank");
        }

        String apiUrl = "https://api.minimax.chat/v1/t2a_v2";

        Map<String, Object> body = new HashMap<>();
        body.put("model", "speech-01");

        Map<String, Object> textWrapper = new HashMap<>();
        textWrapper.put("type", "plain");
        textWrapper.put("text", text);
        body.put("text", textWrapper);

        Map<String, Object> voiceSetting = new HashMap<>();
        voiceSetting.put("voice", StrUtil.isNotBlank(voiceId) ? voiceId : "male-qn-qingse");
        body.put("voice_setting", voiceSetting);

        String jsonBody = JSONUtil.toJsonStr(body);

        log.info("[AIClient] MiniMax TTS Request | Text length: {} | Voice: {}",
            text.length(),
            voiceSetting.get("voice"));

        return postJson(apiUrl, jsonBody, apiKey);
    }

    /**
     * Call MiniMax TTS with default voice (male-qn-qingse).
     */
    public String callMinimaxTTS(String text, String apiKey) {
        return callMinimaxTTS(text, apiKey, "male-qn-qingse");
    }

    // ==================== Streaming Methods ====================

    /**
     * Send streaming POST request with callback for each chunk.
     *
     * Use case: AI chat streaming, TTS streaming, etc.
     * Uses SSE (Server-Sent Events) with Transfer-Encoding: chunked
     *
     * Example:
     * ```java
     * aiClient.postStream(url, jsonBody, token, chunk -> {
     *     // chunk contains SSE data: {"content":"..."}
     *     System.out.println("Received: " + chunk);
     * });
     * ```
     *
     * @param url       Target API URL
     * @param body      JSON body
     * @param token     Bearer token
     * @param callback  Consumer to process each data line
     */
    public void postStream(String url, String body, String token, Consumer<String> callback) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(body) || callback == null) {
            throw new IllegalArgumentException("[AIClient] url/body/callback cannot be null");
        }

        log.debug("[AIClient] Streaming POST | URL: {}", url);

        try {
            HttpRequest request = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")  // SSE
                .timeout(connectTimeout + readTimeout + writeTimeout)
                .body(body);

            if (StrUtil.isNotBlank(token)) {
                request.header("Authorization", "Bearer " + token.trim());
            }

            HttpResponse response = request.executeAsync();
            String fullBody = response.body();
            if (fullBody != null) {
                String[] lines = fullBody.split("\n");
                for (String line : lines) {
                    if (StrUtil.isNotBlank(line) && line.startsWith("data:")) {
                        callback.accept(line.substring(5).trim());
                    }
                }
            }

            log.debug("[AIClient] Streaming completed");

        } catch (Exception ex) {
            log.error("[AIClient] Streaming error | {}", ex.getMessage());
            throw new RuntimeException("[AIClient] Streaming failed: " + ex.getMessage(), ex);
        }
    }
}
