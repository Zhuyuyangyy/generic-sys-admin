package com.zyy.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensures every authority named in source also exists in the RBAC seed.
 *
 * History: `OperationLogController` required `sys:log:list` while the database
 * and `PermConst` define `system:log:list` — every administrator would have got
 * a 403. This test fails on any such drift.
 */
@DisplayName("Authority 与 seed 一致性测试")
class AuthoritySeedConsistencyTest {

    private static final Pattern AUTHORITY =
            Pattern.compile("@ss\\.hasAuthority\\('([a-z]+:[a-zA-Z:]+)'\\)");

    /**
     * Surefire runs with the CWD set to the module directory ({@code backend/}),
     * so the module root is that directory, not the repo root. Probing upward
     * used to work when migrations lived in a repo-root {@code sql/}; it broke
     * once they moved to the Flyway standard location under this module.
     */
    private static final Path MODULE_ROOT = Path.of("").toAbsolutePath();
    private static final Path CONTROLLER_DIR =
            MODULE_ROOT.resolve("src/main/java/com/zyy/controller");
    private static final Path MIGRATION =
            MODULE_ROOT.resolve("src/main/resources/db/migration/V1.2__rbac_schema_completion.sql");

    /** Every authority the controllers ask for. */
    private static Set<String> authoritiesInControllers() throws IOException {
        Set<String> found = new HashSet<>();
        try (var walk = Files.walk(CONTROLLER_DIR)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = AUTHORITY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    found.add(m.group(1));
                }
            }
        }
        return found;
    }

    /** Every permission literal seeded by the migration. */
    private static Set<String> permissionsInSeed() throws IOException {
        Set<String> found = new HashSet<>();
        for (String line : Files.readAllLines(MIGRATION, StandardCharsets.UTF_8)) {
            Matcher m = Pattern.compile("'([a-z]+:[a-zA-Z:]+)'").matcher(line);
            while (m.find()) {
                found.add(m.group(1));
            }
        }
        return found;
    }

    /** Every permission literal defined in PermConst. */
    private static Set<String> permissionsInEnum() throws IOException {
        Path permConst = MODULE_ROOT
                .resolve("src/main/java/com/zyy/enums/PermConst.java");
        Set<String> found = new HashSet<>();
        for (String line : Files.readAllLines(permConst, StandardCharsets.UTF_8)) {
            int open = line.indexOf('"');
            int close = line.indexOf('"', open + 1);
            if (open > 0 && close > open) {
                String literal = line.substring(open + 1, close);
                if (literal.contains(":")) {
                    found.add(literal);
                }
            }
        }
        return found;
    }

    @Test
    @DisplayName("所有 @ss.hasAuthority 权限名都必须存在于 seed 中")
    void everyAuthorityIsSeeded() throws IOException {
        Set<String> inCode = authoritiesInControllers();
        Set<String> inSeed = permissionsInSeed();

        assertFalse(inCode.isEmpty(), "no authorities found — did the controller path change?");

        List<String> missing = new ArrayList<>(inCode);
        missing.removeAll(inSeed);
        assertTrue(missing.isEmpty(),
                "这些权限在 Controller 中被要求，但没有 seed： " + missing
                        + "（管理员会被 403）");
    }

    @Test
    @DisplayName("seed 权限名必须与 PermConst 一致")
    void seedMatchesEnum() throws IOException {
        Set<String> inSeed = permissionsInSeed();
        Set<String> inEnum = permissionsInEnum();

        List<String> notInEnum = new ArrayList<>(inSeed);
        notInEnum.removeAll(inEnum);
        assertTrue(notInEnum.isEmpty(),
                "seed 中的权限名不在 PermConst： " + notInEnum);
    }

    @Test
    @DisplayName("所有 Controller 权限都必须来自 @ss.hasAuthority，而不是裸 hasAuthority")
    void noBareHasAuthorityInControllers() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (var walk = Files.walk(CONTROLLER_DIR)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.contains("@PreAuthorize")
                            && line.contains("hasAuthority(")
                            && !line.contains("@ss.hasAuthority(")) {
                        offenders.add(file.getFileName() + ": " + line.trim());
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些写法绕过 SecurityChecker（SpEL principal 限制）： " + offenders);
    }
}
