package com.zyy.voice;

import cn.hutool.core.util.IdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * ==========================================================
 * MOCK TTS 莽聙鹿莽聜碌氓鹿聡茅聰聸氓聽聼莽麓篓莽禄聙?/ 茅聫聝?Key 茅聬聵猫聢碌芒聜卢盲陆路莽卢聟茅聴聞氓露聡茅陋聡茅聫聜猫搂聞卯聰聧茅聰聸? * ==========================================================
 *
 * 茅聫聧莽篓驴莽赂戮忙碌聽氓鲁掳芒聜卢莽隆路莽麓掳
 * - 氓篓聦芒聞聝忙鹿聛 MiniMax Key 忙露聰莽聠禄氓聟聵莽聮潞忙聢娄芒聜卢忙掳颅忙職拢忙露聯卯聛聠茅聝麓莽录聛? * - 猫陇掳芒聲聞氓拢聤茅聫聝茫聝楼莽鹿聰氓娄炉芒聞聝氓芦聶莽聮聡卯聟垄莽聟露茅聨戮卯聟聼氓搂陇茅聰聸氓虏聝莽聵聨忙驴庐忙聴聜忙鹿聟莽聙鹿氓卤陆莽聲卢茅聧聫茫聞娄忙拢陇茅聨掳莽聠潞莽聟隆
 * - 氓炉庐芒聜卢茅聧聶忙聢娄忙篓聛氓篓聢氓聟赂莽卢聣茅聬垄茫聞篓氓搂鲁茅聳陆忙聠聥莽麓聺猫鹿聡卯聜拢卯聞聻忙聺漏卯聟聺氓聰卢忙楼聽氓虏聝莽聵聣
 *
 * 茅聭路卯聛聞氓搂漏茅聧職卯聢聹忙聲陇茅聫聣芒聙虏忙卢垄茅聰聸? * 1. 茅聳掳氓露聡莽聳聠 tts.provider=mock茅聰聸氓聽聼忙篓聣氓炉庐氓驴聯氓聻聫茅聨鹿卯聺庐莽麓職
 * 2. 忙露聯氓露聣氓聨陇莽录聝?tts.provider茅聰聸氓卤录莽卢聳 minimax.api-key 忙露聯猫聶鹿芒聰聳 茅聢芦?茅聭路卯聛聞氓搂漏茅聧職卯聢聹忙聲陇 Mock
 *
 * 忙聺聢忙聮鲁氓職颅莽禄聙猫陆掳莽路楼茅聰聸? * [MOCK TTS] 莽禄炉猫聧陇莽虏潞氓娄炉芒聞聝氓芦聶茅聨戮卯聟聼氓搂陇
 *   茅聫聜氓聸娄忙卢垄氓庐赂氓聫聣氓聻職茅聧聰莽聠路忙聜聯氓搂聺茫聝篓氓職娄茅聧聮氓聴聴莽芦路氓炉庐氓驴聨莽掳炉莽聙聸忙篓潞氓聛聧
 *   茅聴聤氓庐聽氓拢聤: male-qn-qingse茅聰聸氓聽楼忙陆職茅陋聻氓炉赂忙聲潞忙戮鹿氓赂庐莽麓聺氓篓聯氓聭庐莽路垄茅聰聸? *   茅聭掳忙楼聛忙陇聜: 0.001 ms
 */
@Component
@ConditionalOnProperty(name = "tts.provider", havingValue = "mock", matchIfMissing = false)
public class MockTtsServiceImpl implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(MockTtsServiceImpl.class);

    @Value("${tts.mock.delay-ms:0}")
    private long mockDelayMs;

    private static final List<MockVoice> MOCK_VOICES = Arrays.asList(
        new MockVoice("male-qn-qingse", "茅聴聢忙聨聭氓聥戮茅聬垄氓鲁掳茂录聬", "氓篓聯氓聭庐莽路垄茅聨麓忙聞庐氓聲聸茅聰聸氓虏聞芒聜卢氓聜職忙聜聨茅聧聼氓聴聴氓搂聼"),
        new MockVoice("female-shaonv", "茅聮聙忙驴聤氓赂聙莽聛聫忙聢聺茫聜鲁", "茅聫聦忙聴聜莽路篓氓篓聯氓聭掳氓聣垄茅聰聸氓虏聞芒聜卢氓聜職忙聜聨茅聫聛忙卢聮氓聛聸"),
        new MockVoice("male-zhongnian", "忙露聯卯聟聻氓聥戮茅聬垄氓鲁掳茂录聬", "氓篓聦氓陇聦脟聰茅聫聢氓陇聤氓搂聫茅聰聸氓虏聞芒聜卢氓聜職忙聜聨莽聮搂氓聥庐卯聠聠"),
        new MockVoice("female-yanting", "茅聬聲忙聸隆卯聡垄茅聭戮氓聯聞茂录聬", "氓篓聯芒聲聛卯聼搂茅聧聰茫聞楼忙聝聣茅聰聸氓虏聞芒聜卢氓聜職忙聜聨茅聫聜盲录麓忙陇聢"),
        new MockVoice("male-huang", "茅聫聧氓聸搂氓聶炉茅聬垄氓鲁掳茂录聬", "氓篓聯氓聭庐忙芦職茅聭路卯聛聠氓聤搂茅聰聸氓虏聞芒聜卢忙掳卤忙聲陇茅聧娄莽聝聵忙芦聶")
    );

    private record MockVoice(String voiceId, String name, String scene) {}

    @Override
    public String synthesize(String text, String voiceId, Double speed, Double volume, Double pitch) {
        long startTime = System.currentTimeMillis();

        if (mockDelayMs > 0) {
            try { Thread.sleep(mockDelayMs); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        String resolvedVoiceId = (voiceId != null && !voiceId.isBlank()) ? voiceId : "male-qn-qingse";
        MockVoice voice = findVoice(resolvedVoiceId);
        long cost = System.currentTimeMillis() - startTime;

        printMockBroadcast(text, voice, speed, volume, pitch, cost);

        String mockAudioPath = buildMockAudioPath(text);
        log.info("[MOCK TTS] 氓娄炉芒聞聝氓芦聶茅聴聤忙聤陆卯聲露莽聮潞卯聢職莽路聻茅聰聸氓聽聹莽虏聨茅聫聢卯聞聙忙鹿麓氓漏聲忙聴聜茫聛職茅聬垄卯聼聮莽麓職: {}", mockAudioPath);
        return mockAudioPath;
    }

    @Override
    public boolean isMockMode() { return true; }

    @Override
    public String getProviderName() { return "MockTts (模拟TTS，无Key状态)"; }

    private void printMockBroadcast(String text, MockVoice voice,
                                    Double speed, Double volume, Double pitch,
                                    long costMs) {
        String speedStr  = speed  != null ? String.format("%.1fx", speed) : "1.0x";
        String volStr    = volume != null ? String.format("%.0f%%", volume) : "50%";
        String pitchStr  = pitch  != null ? String.format("%.1f", pitch) : "1.0";

        String divider = "\u2500".repeat(54);
        String thinDivider = "\u2500".repeat(54);

        log.info("");
        log.info("\u001b[36m茅聢鹿氓卤赂忙聰垄\u001b[35m \u001b[1m[MOCK TTS] 莽禄炉猫聧陇莽虏潞氓娄炉芒聞聝氓芦聶茅聨戮卯聟聼氓搂陇\u001b[0m \u001b[36m茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢\u001b[0m");
        log.info("\u001b[36m茅聢鹿盲驴聯u001b[0m  \u001b[32m  \"{}\"\u001b[0m", text);
        log.info("\u001b[36m茅聢鹿盲驴聯u001b[0m  \u001b[90m 茅聴聤氓庐聽氓拢聤: {}茅聰聸氓聺陆}茅聰聸氓垄聤 莽聮聡卯聟垄芒聜卢? {} | 茅聴聤忙聤陆氓聶潞: {} | 茅聴聤氓庐聽莽職聼: {}\u001b[0m", voice.name(), voice.scene(), speedStr, volStr, pitchStr);
        log.info("\u001b[36m茅聢鹿盲驴聯u001b[0m  \u001b[90m 茅聭掳忙楼聛忙陇聜: {} ms\u001b[0m", costMs);
        log.info("\u001b[36m茅聢鹿忙聳潞忙聰垄茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢茅聢鹿芒聜卢\u001b[0m");
        log.info("");
    }

    private String buildMockAudioPath(String text) {
        String uuid = IdUtil.fastSimpleUUID();
        return "/mock/audio/" + uuid + ".mp3";
    }

    private MockVoice findVoice(String voiceId) {
        return MOCK_VOICES.stream()
            .filter(v -> v.voiceId().equals(voiceId))
            .findFirst()
            .orElse(MOCK_VOICES.get(0));
    }

    @PostConstruct
    public void init() {
        log.info("");
        log.info("\u001b[35m茅聢潞忙聳潞忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶陆\u001b[0m");
        log.info("\u001b[35m茅聢潞? TTS 忙聺漏忙聞炉卯聰聭氓娄炉芒聙鲁莽麓隆茅聰聸忙颅聛OCK茅聰聸氓聽聼脛聛茅聨路莽聡聜莽麓職                               茅聢潞忙聟颅u001b[0m");
        log.info("\u001b[35m茅聢潞莽聤芦忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙職聨\u001b[0m");
        log.info("\u001b[35m茅聢潞? 猫陇掳忙聮鲁氓垄聽氓篓聦芒聞聝忙鹿聛茅聳掳氓露聡莽聳聠茅聬陋莽聠路莽聳聞茅聬篓?MiniMax API Key茅聰聸氓虏聙茅聝麓莽录聛莽聠禄氓職聹茅聧聰茫聞楼氓聻聫茅聨鹿卯聺颅猫麓聼 Mock  茅聢潞忙聟颅u001b[0m");
        log.info("\u001b[35m茅聢潞? 茅聧職卯聢聹忙聲陇茅聬陋莽聠路莽聳聞 TTS 茅聬篓氓聥卢忙聼聼氓炉庐氓驴楼莽麓掳                                    茅聢潞忙聟颅u001b[0m");
        log.info("\u001b[35m茅聢潞?   1. 茅聧娄?application-dev.yml 忙露聯卯聟垄氓聨陇莽录聝?minimax.api-key       茅聢潞忙聟颅u001b[0m");
        log.info("\u001b[35m茅聢潞?   2. 莽聮聛氓聣搂莽聳聠 tts.provider=minimax茅聰聸氓聽聺氓陆虏茅聳芦氓陇聸莽麓職                  茅聢潞忙聟颅u001b[0m");
        log.info("\u001b[35m茅聢潞?   3. 茅聳虏氓露聟忙聝聨茅聧職氓潞拢卯聛卢茅聰聸氓卤戮氓赂露茅聧聮猫路潞氓陆麓猫陇掳芒聲聞氓拢聤茅聫聝茫聝楼莽鹿聰莽聛聫氓聴聴氓陆聣忙露聯猫聶鹿忙鹿隆莽聙鹿莽聜露卯聡垄茅聴聤氓聠虏忙聜聨茅聨麓忙聞炉卯聡卢氓搂鹿?      茅聢潞忙聟颅u001b[0m");
        log.info("\u001b[35m茅聢潞忙掳拢忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙聶虏茅聢潞忙聞篓忙職聠\u001b[0m");
        log.info("");
    }
}
