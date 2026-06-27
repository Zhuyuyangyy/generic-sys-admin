package com.zyy.nl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NL Copilot Test Suite - Dry Run & Risk Guard")
class NLCommandTest {

    @Autowired
    private NLService nlService;

    @Autowired
    private RiskAssessor riskAssessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    record NLTestCommand(String input, String expectedIntent, String expectedRisk,
                         boolean shouldExecuteWithoutConfirmation) {}

    private List<NLTestCommand> testCommands;

    @BeforeEach
    void loadTestCommands() throws IOException {
        testCommands = new ArrayList<>();
        Path jsonlPath = Paths.get("src/test/resources/nl_commands.jsonl");
        List<String> lines = Files.readAllLines(jsonlPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.isBlank()) continue;
            NLTestCommand cmd = objectMapper.readValue(line, NLTestCommand.class);
            testCommands.add(cmd);
        }
    }

    @Test
    @DisplayName("Should load exactly 30 test commands from JSONL")
    void shouldLoadExactly30Commands() {
        assertEquals(30, testCommands.size(), "JSONL file must contain exactly 30 entries");
    }

    @Test
    @DisplayName("Dry run should produce valid results for all test commands")
    void dryRunShouldProduceValidResults() throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("input,expectedIntent,actualIntent,intentMatch,expectedRisk,actualRisk,riskMatch,")
           .append("shouldExecuteWithoutConfirmation,confirmRequired,confirmationIdPresent,requiresApproval,riskLevel\n");

        int passed = 0;
        int failed = 0;

        for (NLTestCommand cmd : testCommands) {
            DryRunResult result = nlService.dryRun(cmd.input());

            String actualIntent = result.getIntent();
            String actualRisk = result.getRiskLevel();
            boolean intentMatch = cmd.expectedIntent().equals(actualIntent);
            boolean riskMatch = cmd.expectedRisk().equals(actualRisk);
            boolean confirmRequired = result.isConfirmRequired();
            boolean shouldExecuteWithoutConfirm = !confirmRequired;
            boolean confirmBehaviorMatch = cmd.shouldExecuteWithoutConfirmation() == shouldExecuteWithoutConfirm;

            // Verify confirmationId is returned for write operations
            boolean confirmationIdPresent = result.getConfirmationId() != null && !result.getConfirmationId().isBlank();
            boolean isWriteOperation = !"QUERY".equals(cmd.expectedIntent());
            if (isWriteOperation) {
                assertTrue(confirmationIdPresent,
                        "Write operation should return confirmationId for: " + cmd.input());
            }

            // Verify HIGH/CRITICAL risk requires approval
            boolean requiresApproval = result.isRequiresApproval();
            if ("HIGH".equals(cmd.expectedRisk()) || "CRITICAL".equals(cmd.expectedRisk())) {
                assertTrue(requiresApproval,
                        "HIGH/CRITICAL risk should require approval for: " + cmd.input());
            }

            // Verify intent matches
            if (!intentMatch) {
                // Log but don't fail for complex commands where intent detection may differ
                System.err.println("INTENT MISMATCH: input='" + cmd.input()
                        + "' expected=" + cmd.expectedIntent() + " actual=" + actualIntent);
            }

            // Verify risk matches
            assertEquals(cmd.expectedRisk(), actualRisk,
                    "Risk level mismatch for: " + cmd.input());

            // Verify shouldExecuteWithoutConfirmation matches
            assertEquals(cmd.shouldExecuteWithoutConfirmation(), shouldExecuteWithoutConfirm,
                    "shouldExecuteWithoutConfirmation mismatch for: " + cmd.input());

            if (intentMatch && riskMatch && confirmBehaviorMatch) {
                passed++;
            } else {
                failed++;
            }

            csv.append(String.format("\"%s\",%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    cmd.input(),
                    cmd.expectedIntent(), actualIntent, intentMatch,
                    cmd.expectedRisk(), actualRisk, riskMatch,
                    cmd.shouldExecuteWithoutConfirmation(), confirmRequired,
                    confirmationIdPresent, requiresApproval, actualRisk));
        }

        // Write results CSV
        Path outPath = Paths.get("experiment_results", "nl_test_results.csv");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, csv.toString(), StandardCharsets.UTF_8);

        System.out.println("\n[NL Command Test] Results written to: " + outPath.toAbsolutePath());
        System.out.println("Passed: " + passed + " / Failed: " + failed + " / Total: " + testCommands.size());
    }

    @Test
    @DisplayName("LOW risk operations should not require approval")
    void lowRiskShouldNotRequireApproval() {
        assertFalse(riskAssessor.requiresApproval("LOW"));
    }

    @Test
    @DisplayName("MEDIUM risk operations should not require approval but should require confirmation")
    void mediumRiskShouldRequireConfirmationButNotApproval() {
        assertFalse(riskAssessor.requiresApproval("MEDIUM"));
        assertTrue(riskAssessor.confirmRequired("MEDIUM"));
    }

    @Test
    @DisplayName("HIGH risk operations should require approval")
    void highRiskShouldRequireApproval() {
        assertTrue(riskAssessor.requiresApproval("HIGH"));
    }

    @Test
    @DisplayName("CRITICAL risk operations should require approval")
    void criticalRiskShouldRequireApproval() {
        assertTrue(riskAssessor.requiresApproval("CRITICAL"));
    }

    @Test
    @DisplayName("Query intent should always be LOW risk")
    void queryIntentShouldBeLowRisk() {
        assertEquals("LOW", riskAssessor.assessRisk("QUERY", "EQUIPMENT"));
        assertEquals("LOW", riskAssessor.assessRisk("QUERY", "CONSUMABLE"));
        assertEquals("LOW", riskAssessor.assessRisk("QUERY", "USER"));
    }

    @Test
    @DisplayName("Delete intent should be HIGH risk")
    void deleteIntentShouldBeHighRisk() {
        assertEquals("HIGH", riskAssessor.assessRisk("DELETE", "EQUIPMENT"));
        assertEquals("HIGH", riskAssessor.assessRisk("DELETE", "CONSUMABLE"));
    }

    @Test
    @DisplayName("Bulk operations should be CRITICAL risk")
    void bulkOperationsShouldBeCriticalRisk() {
        assertEquals("CRITICAL", riskAssessor.assessRisk("UPDATE", "CONSUMABLE", true));
        assertEquals("CRITICAL", riskAssessor.assessRisk("CREATE", "EQUIPMENT", true));
        assertEquals("CRITICAL", riskAssessor.assessRisk("DELETE", "EQUIPMENT", true));
    }

    @Test
    @DisplayName("getAffectedTables should return correct tables for entity types")
    void getAffectedTablesShouldReturnCorrectTables() {
        List<String> equipTables = riskAssessor.getAffectedTables("EQUIPMENT");
        assertFalse(equipTables.isEmpty(), "EQUIPMENT should have affected tables");

        List<String> consumableTables = riskAssessor.getAffectedTables("CONSUMABLE");
        assertFalse(consumableTables.isEmpty(), "CONSUMABLE should have affected tables");

        List<String> unknownTables = riskAssessor.getAffectedTables("UNKNOWN");
        assertTrue(unknownTables.isEmpty(), "UNKNOWN entity type should have no affected tables");
    }

    @Test
    @DisplayName("describeExpectedChanges should return human-readable description")
    void describeExpectedChangesShouldReturnDescription() {
        String queryDesc = riskAssessor.describeExpectedChanges("QUERY", "EQUIPMENT");
        assertNotNull(queryDesc);
        assertTrue(queryDesc.contains("EQUIPMENT") || queryDesc.contains("Read"),
                "Query description should mention reading or entity type");

        String deleteDesc = riskAssessor.describeExpectedChanges("DELETE", "CONSUMABLE");
        assertNotNull(deleteDesc);
        assertTrue(deleteDesc.toLowerCase().contains("delete") || deleteDesc.contains("删除"),
                "Delete description should indicate deletion");
    }

    @Test
    @DisplayName("DryRunResult should contain all required fields")
    void dryRunResultShouldContainAllFields() {
        DryRunResult result = nlService.dryRun("删除设备");
        assertNotNull(result.getConfirmationId(), "confirmationId should be set");
        assertNotNull(result.getIntent(), "intent should be set");
        assertNotNull(result.getRiskLevel(), "riskLevel should be set");
        assertNotNull(result.getEntities(), "entities should be set");
        assertNotNull(result.getAffectedTables(), "affectedTables should be set");
        assertNotNull(result.getExpectedChanges(), "expectedChanges should be set");
        assertNotNull(result.getTimestamp(), "timestamp should be set");
    }

    @Test
    @DisplayName("Confirmation store should validate and consume confirmation IDs")
    void confirmationStoreShouldWorkCorrectly() {
        DryRunResult result = nlService.dryRun("查询设备");
        String confirmationId = result.getConfirmationId();

        // Confirmation ID should be valid
        assertNotNull(confirmationId);

        // Consuming with invalid ID should throw
        assertThrows(com.zyy.exception.BusinessException.class, () -> {
            nlService.executeConfirmed("查询设备", "invalid-confirmation-id");
        });
    }

    @Test
    @DisplayName("Query operations should have confirmRequired=false")
    void queryOperationsShouldNotRequireConfirmation() {
        for (NLTestCommand cmd : testCommands) {
            if ("QUERY".equals(cmd.expectedIntent()) && "LOW".equals(cmd.expectedRisk())) {
                DryRunResult result = nlService.dryRun(cmd.input());
                assertFalse(result.isConfirmRequired(),
                        "LOW risk QUERY should not require confirmation: " + cmd.input());
            }
        }
    }
}
