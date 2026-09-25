package com.zyy.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用真实 MySQL + Flyway 验证 migration 链（Gate C）。
 *
 * <p>这里的断言全部来自"Flyway 自己做了什么"，不是手工 source SQL：</p>
 *
 * <pre>
 * Testcontainers MySQL 8.0.44
 *   → Flyway.info().all()     确认发现的版本
 *   → Flyway.migrate()        确认应用的版本数
 *   → 断言 schema / seed
 *   → Mapper smoke
 *   → 再次 migrate()          必须为 0
 *   → Flyway.validate()       必须通过
 * </pre>
 *
 * <p>没有 Docker 时整个类 {@link Assumptions#abort} （skip），不算失败：
 * CI 有 Docker 会真正执行，本地无 Docker 只损失覆盖而不阻塞构建。</p>
 */
@DisplayName("Flyway + Testcontainers migration 集成测试")
class MigrationIT {

    private static final String IMAGE = "mysql:8.0.44";
    private static final String DB_NAME = "erms_it";

    private static org.testcontainers.containers.MySQLContainer<?> mysql;
    private static DataSource dataSource;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void startContainer() {
        if (!dockerAvailable()) {
            Assumptions.abort("Docker 不可用，跳过 Testcontainers 集成测试");
        }
        mysql = new org.testcontainers.containers.MySQLContainer<>(IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername("it")
                .withPassword("it-password");
        mysql.start();

        dataSource = new DriverManagerDataSource(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void stopContainer() {
        if (mysql != null) {
            mysql.stop();
        }
    }

    private static boolean dockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    // ==================== Gate C ====================

    @Test
    @DisplayName("Flyway 发现仓库中全部 versioned migration")
    void discoversAllMigrations() {
        var all = flyway().info().all();
        List<String> versions = new ArrayList<>();
        for (var info : all) {
            if (info.getVersion() != null) {
                versions.add(info.getVersion().getVersion());
            }
        }
        assertEquals(List.of("1.0", "1.1", "1.2"), versions,
                "Flyway 必须按顺序发现 V1.0 / V1.1 / V1.2");
    }

    @Test
    @DisplayName("空库 migrate 应用全部 migration")
    void migrateAppliesAll() {
        int applied = flyway().migrate().migrationsExecuted;
        assertEquals(3, applied, "空库上应恰好执行 3 个 migration");
    }

    @Test
    @DisplayName("第二次 migrate 应用 0 个（Flyway 语义：versioned 只执行一次）")
    void secondMigrateAppliesNothing() {
        int applied = flyway().migrate().migrationsExecuted;
        assertEquals(0, applied, "重复 migrate 不应执行任何 migration");
    }

    @Test
    @DisplayName("Flyway validate 通过（checksum 未漂移）")
    void validatePasses() {
        assertDoesNotThrow(() -> flyway().validate());
    }

    @Test
    @DisplayName("schema 断言：10 张表全部就位")
    void schemaAssertions() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        for (String expected : List.of(
                "sys_user", "sys_role", "sys_user_role", "sys_role_menu", "sys_menu",
                "sys_operation_log", "sys_equipment", "sys_consumable",
                "sys_inventory_record", "sys_inventory_transaction")) {
            assertTrue(tables.contains(expected), "缺失表: " + expected);
        }
    }

    @Test
    @DisplayName("schema 断言：sys_menu 的 RBAC 三列存在")
    void rbacColumnsExist() {
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_menu'",
                String.class);
        for (String expected : List.of("permission", "menu_type", "is_external")) {
            assertTrue(columns.contains(expected), "sys_menu 缺列: " + expected);
        }
    }

    @Test
    @DisplayName("seed 断言：Controller 使用的每个权限都存在于 sys_menu 且授给 admin")
    void permissionSeedMatchesControllerAuthorities() throws IOException {
        // 1. 从源码取出 Controller 实际引用的权限
        Path controllerDir = Path.of("src/main/java/com/zyy/controller");
        Pattern p = Pattern.compile("@ss\\.hasAuthority\\('([a-z:A-Za-z]+)'\\)");
        java.util.Set<String> required = new java.util.HashSet<>();
        if (Files.isDirectory(controllerDir)) {
            try (var walk = Files.walk(controllerDir)) {
                for (Path f : walk.filter(x -> x.toString().endsWith(".java"))
                        .collect(Collectors.toList())) {
                    Matcher m = p.matcher(Files.readString(f));
                    while (m.find()) {
                        required.add(m.group(1));
                    }
                }
            }
        }
        assertFalse(required.isEmpty(), "没有从源码读到任何权限，路径变了？");

        // 2. 从 seed 取权限
        List<String> seeded = jdbc.queryForList(
                "SELECT DISTINCT permission FROM sys_menu WHERE permission IS NOT NULL",
                String.class);

        List<String> missing = new ArrayList<>(required);
        missing.removeAll(seeded);
        assertTrue(missing.isEmpty(), "这些权限被 Controller 引用但未 seed: " + missing);

        // 3. admin (role_id = 1) 必须拥有每一个有 permission 的菜单
        Integer granted = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu rm "
                        + "JOIN sys_menu m ON m.id = rm.menu_id "
                        + "WHERE rm.role_id = 1 AND m.permission IS NOT NULL",
                Integer.class);
        Integer withPermission = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE permission IS NOT NULL",
                Integer.class);
        assertEquals(withPermission, granted,
                "admin 未获得全部权限菜单，登录后会 403");
    }

    @Test
    @DisplayName("Mapper smoke：SysMenuMapper 的 SELECT m.* 不会因 schema 不一致而炸")
    void mapperSmoke() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_menu m "
                        + "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id "
                        + "WHERE m.is_deleted = 0 AND m.status = 1 AND m.visible = 1",
                Integer.class);
        assertNotNull(count);
        assertTrue(count > 0, "admin 的菜单 join 查询应返回行");
    }

    @Test
    @DisplayName("Mysql 版本与 CI 预期一致")
    void mysqlVersionIsPinned() {
        String version = jdbc.queryForObject("SELECT VERSION()", String.class);
        assertTrue(version.startsWith("8.0."), "实际 MySQL 版本: " + version);
    }
}
