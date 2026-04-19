package com.zyy.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyy.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Minimax TTS 工具类
 * 创新点2：融合 Minimax TTS 的智能语音播报交互
 *
 * ⚠️ 安全提醒：API_KEY 和 GROUP_ID 切勿硬编码！
 * 通过 application.yml 的环境变量(${MINIMAX_API_KEY})注入，交付时提醒客户填入自己的Key
 */
@Slf4j
@Component
public class MinimaxTtsUtil {

    @Value("${minimax.api-url}")
    private String apiUrl;

    @Value("${minimax.app-id}")
    private String appId;

    @Value("${minimax.api-key}")
    private String apiKey;

    @Value("${minimax.group-id}")
    private String groupId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MinimaxTtsUtil(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * 文本转语音（TTS）
     * @param text 待合成文本
     * @param voiceId 音色ID
     * @param speed 语速 0.5~2.0，默认1.0
     * @param volume 音量 0~100，默认50
     * @param pitch 音调 0.5~2.0，默认1.0
     * @return 生成的语音URL
     */
    public String textToSpeech(String text, String voiceId, 
                               Double speed, Double volume, Double pitch) {
        long timestamp = Instant.now().getEpochSecond();
        String signature = generateSignature(timestamp);

        String url = apiUrl + "/t2a_v2";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "speech-02-high-quality");
        requestBody.put("text", text);
        requestBody.put("voice_id", voiceId);
        requestBody.put("speed", speed != null ? speed : 1.0);
        requestBody.put("volume", volume != null ? volume : 50);
        requestBody.put("pitch", pitch != null ? pitch : 1.0);
        requestBody.put("output_format", "mp3");

        try {
            String response = restTemplate.postForObject(url, 
                requestBody, String.class);

            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.has("data") && jsonNode.get("data").has("audio_url")) {
                return jsonNode.get("data").get("audio_url").asText();
            } else {
                throw new BusinessException("TTS语音合成失败：" + jsonNode.toString());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS请求异常: {}", e.getMessage());
            throw new BusinessException("TTS服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 生成签名
     */
    private String generateSignature(long timestamp) {
        try {
            String dataToSign = appId + ":" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new BusinessException("签名生成失败");
        }
    }
}
