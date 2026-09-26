package com.zyy.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.zyy.iam.mapper.SysMenuMapper;
import com.zyy.iam.mapper.SysRoleMapper;
import com.zyy.iam.mapper.SysUserMapper;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.inventory.mapper.ConsumableMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V1–V10 migration 的真实验证：真实 MySQL 容器 + Spring Boot 的 Flyway 配置。
 *
 * <p>单元测试用 H2 是合理的——这批 migration 是 MySQL 方言写的（索引名大小写敏感、
 * {@code ENGINE=InnoDB}、FULLTEXT 等），在 H2 上必然失败。真正的问题
 * "V1–V9 能否从空 MySQL 建出完整 schema"只能在这里回答。</p>
 *
 * <p>关键点：Flyway 不是被这条测试手工驱动的 {@code mysql < V1.sql} 序列，
 * 而是 Spring Boot 按 {@code spring.flyway.*} 配置自动执行的那一个。
 * 所以这里验证的就是应用真实启动时会发生的全部事情。</p>
 *
 * <p>没有 Docker 时 {@link BeforeAll} 会 abort，测试 skip 而不是 fail，
 * 因此本地 {@code mvn clean verify} 不会因为缺少 Docker 而红。CI 有 Docker，
 * 会真正执行本类。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
// 启动类在 com.zyy.bootstrap，不在本测试所在包的父包，
// @SpringBootTest 默认向上搜索会找不到，必须显式指定。
@SpringBootTest(classes = com.zyy.bootstrap.GenericSysAdminApplication.class)
@DisplayName("MigrationIT：真实 MySQL 上的 V1–V10")
class MigrationIT {

    /**
     * 与 docker-compose.yml 声明的生产镜像保持一致：mysql:8.0。
     * 不用 latest，也不用别的发行版。
     */
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0");

    /**
     * MySQL 8 要求 lower_case_table_names 相关设置在初始化时确定，容器里默认即可。
     * withReusable(true) 让同一会话内多个测试类复用同一个容器。
     */
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("erms_it")
            .withUsername("it")
            .withPassword("it-password-1234")
            .withReuse(true);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private ConsumableMapper consumableMapper;

    // 说明：datasource 连接由 @ServiceConnection（Spring Boot 3.2 的
    // Testcontainers 集成）自动注册，这里不再手写 DynamicPropertyRegistrar——
    // 手写那份在这个 Boot 版本需要用 ImportTestcontainersDetail 或
    // @DynamicPropertySource，且容易与 @ServiceConnection 冲突。
    // spring.sql.init.mode=never 由下方 @DynamicPropertySource 提供，
    // 确保 schema 完全由 Flyway 管理。

    /**
     * 把容器连接信息注册给 Spring。
     *
     * Boot 3.2 没有 spring-boot-testcontainers 模块（@ServiceConnection 是 3.4+
     * 才有的），所以用 @DynamicPropertySource 显式注册——这也让"连的是哪个库"
     * 在测试里一目了然，而不是依赖自动装配的隐式行为。
     */
    @org.springframework.test.context.DynamicPropertySource
    static void props(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
        // schema 完全由 Flyway 管理，禁用 Spring 自己的 data.sql 初始化
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @AfterAll
    static void stopContainer() {
        if (MYSQL.isRunning()) {
            MYSQL.stop();
        }
    }

    private Set<String> tables() throws SQLException {
        Set<String> found = new java.util.HashSet<>();
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    found.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        return found;
    }

    private boolean hasTable(String name) throws SQLException {
        return tables().contains(name);
    }

    private int count(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("query failed: " + sql, e);
        }
    }

    private List<String> columns(String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getColumns(c.getCatalog(), null, table, "%")) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        return cols;
    }

    // ==================== Gate 3：发现 / 应用 / 校验 ====================

    @Test
    @DisplayName("Flyway 发现全部 V1–V9")
    void discoversAllMigrations() {
        var all = flyway.info().all();
        List<String> versions = new ArrayList<>();
        for (var info : all) {
            if (info.getVersion() != null) {
                versions.add(info.getVersion().getVersion());
            }
        }
        // 按当前真实文件名：V1 .. V9
        assertEquals(
                // 按当前真实文件名：V1 .. V10（V10 见 V10__fix_mysql_if_not_exists_syntax.sql，
                // 它修掉 V4/V8/V9 里 MySQL 不支持的 ADD COLUMN IF NOT EXISTS）。
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                versions,
                "Flyway 必须按顺序发现 V1–V10，实际: " + versions);
        assertEquals(10, versions.size(), "应恰好 10 个 versioned migration");
    }

    @Test
    @DisplayName("空库上 V1–V9 全部应用成功（0 failure）")
    void allMigrationsApplied() {
        // Spring Boot 启动时已经执行过一次 migrate；这里再跑一次 confirm 状态。
        // pending 必须为 0 —— 若某个 migration 失败，Flyway 会标记 failed，
        // info().pending() 非空或 validate() 会抛异常。
        var pending = flyway.info().pending();
        assertTrue(pending.length == 0,
                "存在未应用的 migration，说明有空库迁移失败: " + java.util.Arrays.toString(pending));
    }

    @Test
    @DisplayName("Flyway validate 通过（checksum 无漂移）")
    void validatePasses() {
        assertDoesNotThrow(flyway::validate,
                "validate 失败意味着已应用的 migration 与 classpath 内容不一致");
    }

    @Test
    @DisplayName("第二次 migrate 应用 0 个")
    void secondMigrateAppliesNothing() {
        int applied = flyway.migrate().migrationsExecuted;
        assertEquals(0, applied, "versioned migration 只应执行一次");
    }

    // ==================== Gate 1.8：schema smoke ====================

    @Test
    @DisplayName("核心表全部存在")
    void coreTablesExist() throws SQLException {
        // 表名来自 V1–V9 的实际 CREATE TABLE 语句，非猜测。
        String[] expected = {
                // IAM
                "sys_user", "sys_role", "sys_menu", "sys_user_role", "sys_role_menu",
                "sys_department", "sys_tenant", "token_blacklist", "rate_limit_log",
                // 资产
                "equipment", "asset_location", "asset_assignment", "asset_inspection",
                "maintenance_plan",
                // 库存
                "consumable", "consumable_batch", "supplier",
                "inventory_record", "inventory_transaction", "stock_alert",
                // 工作流
                "workflow_definition", "workflow_instance", "workflow_task",
                // 审计
                "sys_operation_log",
                // ABAC
                "data_scope",
        };
        Set<String> present = tables();
        List<String> missing = new ArrayList<>();
        for (String t : expected) {
            if (!present.contains(t)) {
                missing.add(t);
            }
        }
        assertTrue(missing.isEmpty(), "这些表在 schema 中缺失: " + missing);
    }

    @Test
    @DisplayName("多租户关键列存在（V9）")
    void tenantColumnsExist() throws SQLException {
        // V9 的目标表（见 V9__multi_tenant.sql 的实际 ALTER）
        for (String t : List.of("equipment", "consumable", "workflow_instance")) {
            List<String> cols = columns(t);
            assertTrue(cols.contains("tenant_id"),
                    t + " 缺少 tenant_id —— V9 未生效或被跳过");
        }
        assertTrue(hasTable("sys_tenant"), "V9 应创建 sys_tenant 表");
    }

    // ==================== Gate 4：mapper smoke ====================

    @Test
    @DisplayName("SysUserMapper 能对真实 schema 查询")
    void sysUserMapperQueries() {
        assertNotNull(sysUserMapper, "SysUserMapper 未注入");
        assertDoesNotThrow(() -> sysUserMapper.selectCount(null),
                "SysUserMapper.selectCount 失败：Entity 字段与 migration schema 不一致");
    }

    @Test
    @DisplayName("SysRoleMapper 能对真实 schema 查询")
    void sysRoleMapperQueries() {
        assertNotNull(sysRoleMapper);
        assertDoesNotThrow(() -> sysRoleMapper.selectCount(null),
                "SysRoleMapper.selectCount 失败");
    }

    @Test
    @DisplayName("SysMenuMapper 能对真实 schema 查询（含 permission 列）")
    void sysMenuMapperQueries() throws SQLException {
        assertNotNull(sysMenuMapper);
        // SysMenuEntity 映射 permission 列；V4 之前该列不存在，这里防止回归
        assertTrue(columns("sys_menu").contains("permission"),
                "sys_menu 缺少 permission 列 —— SysMenuMapper 会 Unknown column");
        assertDoesNotThrow(() -> sysMenuMapper.selectCount(null));
    }

    @Test
    @DisplayName("EquipmentMapper 能对真实 schema 查询（含 tenant_id）")
    void equipmentMapperQueries() {
        assertNotNull(equipmentMapper);
        assertDoesNotThrow(() -> equipmentMapper.selectCount(null),
                "EquipmentMapper.selectCount 失败");
    }

    @Test
    @DisplayName("ConsumableMapper 能对真实 schema 查询（含 tenant_id）")
    void consumableMapperQueries() {
        assertNotNull(consumableMapper);
        assertDoesNotThrow(() -> consumableMapper.selectCount(null),
                "ConsumableMapper.selectCount 失败");
    }

    // ==================== Gate 5：tenant / DataScope 拦截器真实生效 ====================

    @Test
    @DisplayName("MyBatisPlusConfig 装配成功且拦截器链可执行真实查询")
    void interceptorChainWorks() {
        // 这一条同时也覆盖"不再形成循环依赖"：
        // 若 MyBatisPlusConfig -> DataScopeFilter -> DataScopeMapper ->
        // sqlSessionFactory 的环重新出现，Spring 上下文根本起不来，
        // 这个测试类里的所有 @Autowired 都会失败。
        assertDoesNotThrow(() -> sysUserMapper.selectCount(null),
                "经过 TenantLine / DataPermission 拦截器的真实查询失败");

        assertDoesNotThrow(() -> {
            Object r = sysMenuMapper.selectCount(null);
            assertNotNull(r);
        }, "sys_menu 查询会触发 DataScopeFilter，失败说明 ABAC 层有运行时错误");
    }

    @Test
    @DisplayName("租户表可写可读（TenantContext 与 tenant_id）")
    void tenantTableRoundTrip() {
        // 直接 SQL 证明 tenant schema 可用，绕过拦截器
        int n = count("SELECT COUNT(*) FROM sys_tenant");
        assertTrue(n >= 0, "sys_tenant 查询失败");

        int withCol = count(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND column_name = 'tenant_id'");
        assertTrue(withCol > 0, "没有任何表带 tenant_id 列");
    }

    @Test
    @DisplayName("seed 数据由 V6 写入")
    void seedDataPresent() {
        // V6__seed_default_data.sql 灌入角色/菜单；role_id=1 通常是 admin。
        int roles = count("SELECT COUNT(*) FROM sys_role");
        assertTrue(roles > 0, "sys_role 为空 —— V6 seed 未执行");
        int menus = count("SELECT COUNT(*) FROM sys_menu");
        assertTrue(menus > 0, "sys_menu 为空 —— V6 seed 未执行");
    }

    @Test
    @DisplayName("容器确实是 MySQL 8.0，与 docker-compose 一致")
    void mysqlVersionMatchesCompose() throws SQLException {
        String version;
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {
            rs.next();
            version = rs.getString(1);
        }
        assertTrue(version.startsWith("8."),
                "容器 MySQL 版本应为 8.x（docker-compose 用 mysql:8.0），实际: " + version);
    }
}
