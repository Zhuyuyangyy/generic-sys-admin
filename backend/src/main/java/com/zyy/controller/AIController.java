package com.zyy.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zyy.client.AIClient;
import com.zyy.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 服务接口
 * <p>
 * 统一封装 MiniMax 多模态 AI 能力（语音合成、图像生成、视频生成）
 *
 * @author Alice
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Services", description = "MiniMax 多模态 AI 服务：语音合成、图像生成、视频生成")
public class AIController {

    private final AIClient aiClient;

    @Value("${minimax.api-key:}")
    private String minimaxApiKey;

    @Value("${minimax.api-url:https://api.us-west-2.modal.direct}")
    private String minimaxApiUrl;

    // ==================== 语音合成 (TTS) ====================

    /**
     * MiniMax 语音合成
     * 模型：speech-01
     */
    @PostMapping("/tts")
    @Operation(summary = "语音合成", description = "调用 MiniMax speech-01 模型将文本转为语音")
    @PreAuthorize("@ss.hasAuthority('ai:tts')")
    public Result<Map<String, String>> tts(
            @Parameter(description = "合成文本") @RequestParam String text,
            @Parameter(description = "音色选择") @RequestParam(defaultValue = "male-qn-qingse") String voice) {

        log.info("[AI] TTS request | textLength={} | voice={}", text.length(), voice);

        if (text.isBlank()) {
            return Result.fail("文本不能为空");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "speech-01");
            body.put("text", Map.of("type", "plain", "text", text));
            body.put("voice_setting", Map.of("voice", voice));

            String response = aiClient.postJson(
                    minimaxApiUrl + "/v1/t2a_v2",
                    JSONUtil.toJsonStr(body),
                    minimaxApiKey
            );

            JSONObject json = JSONUtil.parseObj(response);
            String audioUrl = json.getByPath("data.audio_url", String.class);

            Map<String, String> result = new HashMap<>();
            result.put("audioUrl", audioUrl != null ? audioUrl : "");
            result.put("audioFileId", json.getByPath("data.audio_file_id", String.class));
            result.put("model", "speech-01");

            return Result.ok(result, "语音合成成功");
        } catch (Exception e) {
            log.error("[AI] TTS error", e);
            return Result.fail("语音合成失败：" + e.getMessage());
        }
    }

    // ==================== 图像生成 (Image Generation) ====================

    /**
     * MiniMax 图像生成
     * 模型：image-01
     */
    @PostMapping("/image")
    @Operation(summary = "图像生成", description = "调用 MiniMax image-01 模型根据文本描述生成图像")
    @PreAuthorize("@ss.hasAuthority('ai:image')")
    public Result<Map<String, String>> generateImage(
            @Parameter(description = "图像描述（英文效果更佳）") @RequestParam String prompt,
            @Parameter(description = "图像尺寸：1:1/16:9/9:16/3:4/4:3") @RequestParam(defaultValue = "1:1") String aspectRatio) {

        log.info("[AI] Image generation request | promptLength={}", prompt.length());

        if (prompt.isBlank()) {
            return Result.fail("描述不能为空");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "image-01");
            body.put("prompt", prompt);

            // 尺寸映射
            String size;
            switch (aspectRatio) {
                case "16:9" -> size = "1536x1024";
                case "9:16" -> size = "1024x1536";
                case "3:4" -> size = "1024x1365";
                case "4:3" -> size = "1365x1024";
                default -> size = "1024x1024";
            }
            body.put("size", size);

            String response = aiClient.postJson(
                    minimaxApiUrl + "/v1/images/generations",
                    JSONUtil.toJsonStr(body),
                    minimaxApiKey
            );

            JSONObject json = JSONUtil.parseObj(response);
            String imageUrl = json.getByPath("data[0].url", String.class);

            Map<String, String> result = new HashMap<>();
            result.put("imageUrl", imageUrl != null ? imageUrl : "");
            result.put("revisedPrompt", json.getByPath("data[0].revised_prompt", String.class));
            result.put("model", "image-01");

            return Result.ok(result, "图像生成成功");
        } catch (Exception e) {
            log.error("[AI] Image generation error", e);
            return Result.fail("图像生成失败：" + e.getMessage());
        }
    }

    // ==================== 视频生成 (Video Generation) ====================

    /**
     * MiniMax 视频生成
     * 模型：video-01
     */
    @PostMapping("/video/generate")
    @Operation(summary = "视频生成", description = "调用 MiniMax video-01 模型根据文本描述生成视频（异步，提交后返回 jobId）")
    @PreAuthorize("@ss.hasAuthority('ai:video')")
    public Result<Map<String, String>> generateVideo(
            @Parameter(description = "视频描述") @RequestParam String prompt,
            @Parameter(description = "视频时长（秒）：5/10") @RequestParam(defaultValue = "5") Integer duration) {

        log.info("[AI] Video generation request | promptLength={} | duration={}s", prompt.length(), duration);

        if (prompt.isBlank()) {
            return Result.fail("描述不能为空");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "video-01");
            body.put("prompt", prompt);
            body.put("duration", duration);

            String response = aiClient.postJson(
                    minimaxApiUrl + "/v1/video/generations",
                    JSONUtil.toJsonStr(body),
                    minimaxApiKey
            );

            JSONObject json = JSONUtil.parseObj(response);
            String jobId = json.getByPath("data.job_id", String.class);

            Map<String, String> result = new HashMap<>();
            result.put("jobId", jobId != null ? jobId : "");
            result.put("status", json.getByPath("data.status", String.class));
            result.put("model", "video-01");

            return Result.ok(result, "视频生成任务已提交");
        } catch (Exception e) {
            log.error("[AI] Video generation error", e);
            return Result.fail("视频生成失败：" + e.getMessage());
        }
    }

    /**
     * 查询视频生成进度/结果
     */
    @GetMapping("/video/status/{jobId}")
    @Operation(summary = "查询视频生成状态", description = "根据 jobId 查询视频生成进度及结果 URL")
    @PreAuthorize("@ss.hasAuthority('ai:video')")
    public Result<Map<String, String>> getVideoStatus(
            @Parameter(description = "视频任务 ID") @PathVariable String jobId) {

        try {
            String response = aiClient.get(
                    minimaxApiUrl + "/v1/video/generations/" + jobId,
                    minimaxApiKey
            );

            JSONObject json = JSONUtil.parseObj(response);
            Map<String, String> result = new HashMap<>();
            result.put("jobId", jobId);
            result.put("status", json.getByPath("data.status", String.class));
            result.put("videoUrl", json.getByPath("data.video_url", String.class));

            return Result.ok(result);
        } catch (Exception e) {
            log.error("[AI] Video status error", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    // ==================== 模型列表 ====================

    /**
     * 获取可用 AI 模型列表
     */
    @GetMapping("/models")
    @Operation(summary = "可用模型列表", description = "返回当前配置的 MiniMax 可用模型")
    public Result<Map<String, String[]>> getModels() {
        Map<String, String[]> models = new HashMap<>();
        models.put("tts", new String[]{"speech-01"});
        models.put("image", new String[]{"image-01"});
        models.put("video", new String[]{"video-01"});
        return Result.ok(models);
    }
}
