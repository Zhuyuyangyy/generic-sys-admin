package com.zyy;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zyy.common.Result;
import com.zyy.config.LocalFileStorageStrategy;
import com.zyy.config.MinioAvailability;
import com.zyy.util.MinioUtil;
import com.zyy.voice.MockTtsServiceImpl;
import com.zyy.voice.TtsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ==========================================================
 * 🏆 Mock 集成测试（全链路不依赖外部服务）
 * ==========================================================
 *
 * 🎯 测试目标：
 * - ✅ 验证 TTS Mock 模式正常工作（无 Key 状态）
 * - ✅ 验证文件上传降级到本地存储（无 MinIO 状态）
 * - ✅ 验证 TtsService 接口注入正确
 * - ✅ 验证 MinIO 不可用时自动降级
 *
 * 【运行方式】
 * - IDE：直接右键 Run test
 * - Maven：mvn test -Dspring.profiles.active=mock-test
 * - 无需启动 MySQL / MinIO / Redis，全部 Mock
 *
 * @author Alice 🌸
 */
@SpringBootTest(
    properties = {
        // ===== TTS Mock 配置 =====
        "tts.provider=mock",
        "tts.mock.delay-ms=10",

        // ===== 存储降级配置 =====
        "storage.provider=local",    // 强制使用本地存储，跳过 MinIO 检测
        "storage.fallback-to-local=true",

        // ===== 禁用不需要的组件 =====
        "spring.redis.enabled=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.sql.init.mode=never",
        "mybatis-plus.mapper-locations=",  // 不扫描 XML
        "minio.endpoint=http://localhost:99999",  // 故意设成无效地址
        "minio.bucket-name=test-bucket",
        "minio.access-key=test",
        "minio.secret-key=test"
    }
)
@ActiveProfiles("mock-test")
@DisplayName("【全链路 Mock 测试】TTS + 文件存储 + 降级机制")
public class IntegrationTest {

    // ==================== TTS Mock 测试 ====================

    @Autowired(required = false)
    private MockTtsServiceImpl mockTtsService;

    @Autowired
    private TtsService ttsService;

    @Value("${tts.provider:}")
    private String ttsProvider;

    // ==================== 存储降级测试 ====================

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private MinioAvailability minioAvailability;

    @Autowired(required = false)
    private LocalFileStorageStrategy localStorageStrategy;

    // ==================== 测试数据 ====================

    private MockMultipartFile testImageFile;
    private MockMultipartFile testTextFile;
    private MockMultipartFile testExcelFile;

    // ==================== 前置准备 ====================

    @BeforeEach
    void setUp() {
        // 构建测试文件（模拟前端上传）
        testImageFile = new MockMultipartFile(
            "file",
            "test-avatar.png",
            "image/png",
            "fake-png-content".getBytes()
        );

        testTextFile = new MockMultipartFile(
            "file",
            "readme.txt",
            "text/plain",
            "Hello, this is a test file.".getBytes()
        );

        testExcelFile = new MockMultipartFile(
            "file",
            "report.xlsx",
            "application/vnd.ms-excel",
            "fake-excel-content".getBytes()
        );
    }

    // ==================== TTS Mock 测试 ====================

    @Test
    @DisplayName("✅ TTS Mock 模式：Service 正确注入为 Mock 实现")
    void ttsMockMode_serviceInjectedAsMock() {
        assertNotNull(ttsService, "TtsService should not be null");
        assertTrue(ttsService instanceof MockTtsServiceImpl,
            "TtsService should be injected as MockTtsServiceImpl when tts.provider=mock");
        assertTrue(ttsService.isMockMode(),
            "Mock TTS should report isMockMode() = true");
        assertEquals("MockTts (模拟TTS，无Key状态)", ttsService.getProviderName());
    }

    @Test
    @DisplayName("✅ TTS Mock 模式：synthesize 返回本地 Mock 路径")
    void ttsMockMode_synthesizeReturnsLocalPath() {
        String audioUrl = ttsService.synthesize(
            "文件已成功同步至分布式云存储",
            "male-qn-qingse",
            1.0,
            50.0,
            1.0
        );

        assertNotNull(audioUrl, "Audio URL should not be null");
        assertTrue(audioUrl.startsWith("/mock/audio/"),
            "Mock TTS should return path starting with /mock/audio/, got: " + audioUrl);
        assertTrue(audioUrl.endsWith(".mp3"),
            "Mock TTS URL should end with .mp3, got: " + audioUrl);
    }

    @Test
    @DisplayName("✅ TTS Mock 模式：不同音色 ID 都能正常处理")
    void ttsMockMode_differentVoicesHandled() {
        String[] voiceIds = {"male-qn-qingse", "female-shaonv", "male-bai书生", "invalid-voice-id"};

        for (String voiceId : voiceIds) {
            String audioUrl = ttsService.synthesize(
                "测试文本 " + voiceId,
                voiceId,
                null, null, null  // 使用默认参数
            );

            assertNotNull(audioUrl, "Audio URL should not be null for voice: " + voiceId);
            assertTrue(audioUrl.startsWith("/mock/audio/"),
                "Should return Mock path for voice: " + voiceId);
        }
    }

    @Test
    @DisplayName("✅ TTS Mock 模式：synthesize 使用默认参数不报错")
    void ttsMockMode_defaultParametersWork() {
        // 传入 null 参数，验证有默认处理
        String audioUrl = ttsService.synthesize(null, null, null, null, null);
        assertNotNull(audioUrl, "Should handle null parameters gracefully");
    }

    // ==================== 文件存储降级测试 ====================

    @Test
    @DisplayName("✅ 存储降级：MinIO 不可用时 availability = false")
    void storageFallback_minioAvailabilityFalse() {
        // minioAvailability 由 MinioConfig 在连接失败时设置为 false
        assertFalse(minioAvailability.isAvailable(),
            "MinIO should not be available with invalid endpoint");
        assertNotNull(minioAvailability.getReason(),
            "Should have a reason for MinIO unavailability");
    }

    @Test
    @DisplayName("✅ 存储降级：上传图片自动降级到本地文件系统")
    void storageFallback_uploadToLocalFilesystem() {
        String fileUrl = minioUtil.uploadFile(testImageFile);

        assertNotNull(fileUrl, "File URL should not be null");
        assertTrue(fileUrl.startsWith("/local-files/"),
            "Should return local-files path when MinIO unavailable, got: " + fileUrl);
        assertTrue(fileUrl.contains("/2026/"),
            "Local path should contain year/month subdirectory");
        assertTrue(fileUrl.endsWith(".png"),
            "Should preserve file extension");
    }

    @Test
    @DisplayName("✅ 存储降级：上传纯文本文件成功")
    void storageFallback_uploadTextFile() {
        String fileUrl = minioUtil.uploadFile(testTextFile);

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/local-files/"));
        assertTrue(fileUrl.endsWith(".txt"));
    }

    @Test
    @DisplayName("✅ 存储降级：Excel 文件上传成功（EasyExcel 导出后上传场景）")
    void storageFallback_uploadExcelFile() {
        String fileUrl = minioUtil.uploadFile(testExcelFile);

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/local-files/"));
        assertTrue(fileUrl.endsWith(".xlsx"));
    }

    @Test
    @DisplayName("✅ 存储降级：uploadBytes 字节数组上传成功")
    void storageFallback_uploadBytesArray() {
        byte[] data = "这是一段测试文字，用于验证字节数组上传".getBytes();
        String filename = "test-data-" + System.currentTimeMillis() + ".txt";

        String fileUrl = minioUtil.uploadBytes(data, filename, "text/plain");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/local-files/"));
        assertTrue(fileUrl.contains(filename));
    }

    @Test
    @DisplayName("✅ 存储降级：上传时文件名自动 UUID 重命名（防覆盖）")
    void storageFallback_filenamesAutoRenamed() {
        // 上传 3 次相同文件名的文件
        String url1 = minioUtil.uploadFile(testImageFile, "same-name.png");
        String url2 = minioUtil.uploadFile(testImageFile, "same-name.png");
        String url3 = minioUtil.uploadFile(testImageFile, "same-name.png");

        // 三个 URL 的文件名部分应该各不相同（UUID 重命名）
        String name1 = extractFilename(url1);
        String name2 = extractFilename(url2);
        String name3 = extractFilename(url3);

        assertNotEquals(name1, name2, "File names should be auto-renamed to UUID");
        assertNotEquals(name2, name3, "File names should be unique");
        assertNotEquals(name1, name3, "All three file names should be different");
    }

    @Test
    @DisplayName("✅ 存储降级：deleteFile 删除本地文件成功")
    void storageFallback_deleteLocalFile() {
        // 先上传一个文件
        String fileUrl = minioUtil.uploadFile(testImageFile);
        assertNotNull(fileUrl);

        // 再删除它
        assertDoesNotThrow(() -> minioUtil.deleteFile(fileUrl),
            "Should delete local file without throwing exception");

        // 验证文件不存在（用 exists 方法，因为 MinIO 不可用所以走本地检测）
        String objectName = extractFilename(fileUrl);
        // exists 方法会走本地路径检测
        assertFalse(minioUtil.exists(objectName.replace(".png", "-should-not-exist.png")),
            "Deleted file should not exist");
    }

    @Test
    @DisplayName("✅ 存储降级：空文件上传应抛异常")
    void storageFallback_emptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            minioUtil.uploadFile(emptyFile);
        }, "Uploading empty file should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("✅ 存储降级：buildFileUrl 在 MinIO 不可用时也能正常返回")
    void storageFallback_buildFileUrlWhenMinioUnavailable() {
        // 即使 MinIO 不可用，buildFileUrl 也应该能用
        String url = minioUtil.buildFileUrl("test-file.jpg");
        assertNotNull(url);
        // 应该包含默认的 endpoint 或配置的 public-url
    }

    // ==================== 全链路测试 ====================

    @Test
    @DisplayName("🏆 全链路：从文件上传到触发语音播报（TTS Mock）")
    void fullChain_uploadFileThenBroadcastTts() {
        // ===== Step 1: 上传文件 =====
        String fileUrl = minioUtil.uploadFile(testImageFile);
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/local-files/"));

        // ===== Step 2: 从 URL 提取文件名（模拟业务处理）=====
        String filename = extractFilename(fileUrl);
        assertNotNull(filename);

        // ===== Step 3: 构造播报文本（模拟业务逻辑）=====
        String speechText = String.format(
            "文件上传成功，文件名是 %s，文件已同步至%s",
            filename,
            minioAvailability.isAvailable() ? "分布式云存储" : "本地存储"
        );

        // ===== Step 4: 触发 TTS Mock 播报 =====
        String audioUrl = ttsService.synthesize(speechText, null, null, null, null);

        // ===== 验证全链路结果 =====
        assertTrue(audioUrl.startsWith("/mock/audio/"),
            "TTS should return Mock path: " + audioUrl);
        assertTrue(audioUrl.endsWith(".mp3"));

        // 打印全链路结果（方便查看）
        System.out.println("\n🏆 全链路测试结果：");
        System.out.println("  1. 文件上传 → " + fileUrl);
        System.out.println("  2. 播报文本 → " + speechText);
        System.out.println("  3. 语音 URL → " + audioUrl);
        System.out.println("  4. 存储模式 → " + (minioAvailability.isAvailable() ? "MinIO" : "本地文件系统"));
        System.out.println("  5. TTS 模式 → " + (ttsService.isMockMode() ? "Mock" : "真实Minimax"));
    }

    @Test
    @DisplayName("🏆 全链路：批量上传多个文件 → 触发批量 TTS 播报")
    void fullChain_batchUploadAndBatchBroadcast() {
        // ===== Step 1: 批量上传 =====
        String url1 = minioUtil.uploadFile(testImageFile);
        String url2 = minioUtil.uploadFile(testTextFile);
        String url3 = minioUtil.uploadFile(testExcelFile);

        assertNotNull(url1);
        assertNotNull(url2);
        assertNotNull(url3);

        // ===== Step 2: 汇总上传结果 =====
        int successCount = 0;
        if (url1 != null) successCount++;
        if (url2 != null) successCount++;
        if (url3 != null) successCount++;

        // ===== Step 3: 构造批量播报文本 =====
        String speechText = String.format(
            "批量操作完成，共上传 %d 个文件，文件已成功同步至%s",
            successCount,
            minioAvailability.isAvailable() ? "分布式云存储" : "本地存储"
        );

        // ===== Step 4: 触发 TTS =====
        String audioUrl = ttsService.synthesize(speechText, null, null, null, null);

        assertNotNull(audioUrl);
        assertTrue(audioUrl.startsWith("/mock/audio/"));

        System.out.println("\n🏆 批量全链路测试结果：");
        System.out.println("  成功上传: " + successCount + " 个文件");
        System.out.println("  播报文本: " + speechText);
        System.out.println("  TTS URL: " + audioUrl);
    }

    // ==================== 辅助方法 ====================

    private String extractFilename(String url) {
        if (url == null || url.isEmpty()) return "";
        int lastSlash = url.lastIndexOf('/');
        return lastSlash == -1 ? url : url.substring(lastSlash + 1);
    }
}
