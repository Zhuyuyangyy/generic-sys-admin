package com.zyy.voice;

import com.zyy.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * TTS 语音播报接口
 * 创新点2：融合 Minimax TTS 的智能语音播报交互
 */
@Tag(name = "语音播报", description = "Minimax TTS 语音合成接口")
@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class TtsController {

    private final MinimaxTtsUtil ttsUtil;

    @Operation(summary = "文本转语音")
    @PostMapping("/synthesize")
    public Result<String> synthesize(@Validated @RequestBody TtsRequest request) {
        String audioUrl = ttsUtil.textToSpeech(
            request.getText(),
            request.getVoiceId(),
            request.getSpeed(),
            request.getVolume(),
            request.getPitch()
        );
        return Result.ok(audioUrl, "语音合成成功");
    }
}

// 请求DTO
@lombok.Data
class TtsRequest {
    @Parameter(description = "待合成文本")
    private String text;

    @Parameter(description = "音色ID")
    private String voiceId;

    @Parameter(description = "语速 0.5~2.0")
    private Double speed;

    @Parameter(description = "音量 0~100")
    private Double volume;

    @Parameter(description = "音调 0.5~2.0")
    private Double pitch;
}
