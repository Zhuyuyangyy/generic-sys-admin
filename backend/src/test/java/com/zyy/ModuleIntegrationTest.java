package com.zyy;

import com.zyy.bootstrap.GenericSysAdminApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test to verify Spring context loads successfully
 * with the new domain-driven module structure.
 */
@SpringBootTest(classes = GenericSysAdminApplication.class)
@ActiveProfiles("test")
class ModuleIntegrationTest {

    @Test
    void contextLoads() {
        // Verify that the Spring application context starts successfully
        // with all domain modules properly configured
    }
}
