package com.zyy;

import com.zyy.common.Result;
import com.zyy.config.AppProperties;
import com.zyy.security.JwtUtil;
import com.zyy.service.ConsumableService;
import com.zyy.service.EquipmentService;
import com.zyy.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke Test - Verify Spring Boot application context loads correctly.
 *
 * This test ensures that:
 * - The application context starts without errors
 * - Core beans are properly wired
 * - Configuration properties are loaded
 * - Result utility class works as expected
 *
 * Run with: mvn test -Dtest=SmokeTest
 */
@SpringBootTest(
    properties = {
        "spring.redis.enabled=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:smoketest;MODE=MySQL",
        "spring.sql.init.mode=never",
        "mybatis-plus.mapper-locations=",
        "minio.endpoint=http://localhost:99999",
        "minio.bucket-name=test-bucket",
        "minio.access-key=test",
        "minio.secret-key=test",
        "tts.provider=mock"
    }
)
@DisplayName("Smoke Test - Application Context")
class SmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ConsumableService consumableService;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertNotNull(applicationContext, "Application context should not be null");
    }

    @Test
    @DisplayName("Core service beans are wired")
    void coreBeansWired() {
        assertNotNull(sysUserService, "SysUserService should be injected");
        assertNotNull(equipmentService, "EquipmentService should be injected");
        assertNotNull(consumableService, "ConsumableService should be injected");
        assertNotNull(jwtUtil, "JwtUtil should be injected");
    }

    @Test
    @DisplayName("AppProperties configuration is loaded")
    void appPropertiesLoaded() {
        assertNotNull(appProperties, "AppProperties should be injected");
        assertNotNull(appProperties.getSecurity(), "Security properties should not be null");
        assertNotNull(appProperties.getStorage(), "Storage properties should not be null");
        assertNotNull(appProperties.getAi(), "AI properties should not be null");
        assertNotNull(appProperties.getApp(), "App config should not be null");
    }

    @Test
    @DisplayName("Result utility class works correctly")
    void resultClassWorks() {
        Result<String> success = Result.success("test");
        assertEquals(200, success.getCode());
        assertEquals("test", success.getData());

        Result<Object> fail = Result.fail("error");
        assertEquals(500, fail.getCode());
        assertEquals("error", fail.getMessage());

        Result<Object> ok = Result.ok("data");
        assertEquals(200, ok.getCode());
    }

    @Test
    @DisplayName("JWT utility can generate and validate tokens")
    void jwtUtilWorks() {
        String token = jwtUtil.sign(1L, "smoke-test-user", null);
        assertNotNull(token);
        assertTrue(jwtUtil.validate(token));
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("smoke-test-user", jwtUtil.getUsername(token));
    }

    @Test
    @DisplayName("All controller beans are present in context")
    void controllerBeansPresent() {
        String[] beanNames = applicationContext.getBeanNamesForType(
            org.springframework.web.bind.annotation.RestController.class,
            true, false
        );
        assertTrue(beanNames.length > 0, "At least one controller bean should exist");
    }
}
