package com.zyy.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.audit.mapper.OperationLogMapper;
import com.zyy.audit.model.entity.OperationLogEntity;
import com.zyy.audit.model.vo.AnomalyEvent;
import com.zyy.audit.model.vo.AnomalySummary;
import com.zyy.audit.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of anomaly detection service.
 * <p>
 * Detects the following anomaly types:
 * <ul>
 *   <li>HIGH_FREQUENCY_DELETE - More than 5 delete operations by same user in 1 hour</li>
 *   <li>BULK_OUTBOUND - Outbound of more than 50 units in single transaction</li>
 *   <li>UNAUTHORIZED_ATTEMPT - Repeated 403 responses from same IP</li>
 *   <li>ABNORMAL_LOGIN_TIME - Login outside 6am-10pm from new IP</li>
 *   <li>PRIVILEGE_ESCALATION - User modifying their own role/permissions</li>
 *   <li>CONCURRENT_SESSION - Same user from multiple IPs simultaneously</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final OperationLogMapper operationLogMapper;

    /** In-memory store of detected anomalies (cleared on each scan) */
    private final List<AnomalyEvent> detectedAnomalies = new ArrayList<>();

    /** Known login IPs per user for abnormal login detection */
    private final Map<Long, Set<String>> knownUserIps = new HashMap<>();

    private static final int HIGH_FREQUENCY_DELETE_THRESHOLD = 5;
    private static final int BULK_OUTBOUND_THRESHOLD = 50;
    private static final int UNAUTHORIZED_ATTEMPT_THRESHOLD = 3;
    private static final LocalTime LOGIN_START = LocalTime.of(6, 0);
    private static final LocalTime LOGIN_END = LocalTime.of(22, 0);

    @Override
    @Transactional(readOnly = true)
    public List<AnomalyEvent> detectAnomalies() {
        detectedAnomalies.clear();

        detectHighFrequencyDelete();
        detectBulkOutbound();
        detectUnauthorizedAttempt();
        detectAbnormalLogin();
        detectPrivilegeEscalation();
        detectConcurrentSession();

        log.info("Anomaly scan completed - {} anomalies detected", detectedAnomalies.size());
        return new ArrayList<>(detectedAnomalies);
    }

    @Override
    public AnomalySummary getAnomalySummary() {
        List<AnomalyEvent> anomalies = getRecentAnomalies(24);

        int criticalCount = 0, highCount = 0, mediumCount = 0, lowCount = 0;
        Map<String, Integer> byType = new HashMap<>();

        for (AnomalyEvent event : anomalies) {
            switch (event.getSeverity()) {
                case "CRITICAL" -> criticalCount++;
                case "HIGH" -> highCount++;
                case "MEDIUM" -> mediumCount++;
                case "LOW" -> lowCount++;
            }
            byType.merge(event.getAnomalyType(), 1, Integer::sum);
        }

        return AnomalySummary.builder()
                .totalAnomalies(anomalies.size())
                .criticalCount(criticalCount)
                .highCount(highCount)
                .mediumCount(mediumCount)
                .lowCount(lowCount)
                .byType(byType)
                .build();
    }

    @Override
    public List<AnomalyEvent> getRecentAnomalies(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return detectedAnomalies.stream()
                .filter(e -> e.getDetectedAt() != null && e.getDetectedAt().isAfter(cutoff))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnomalyEvent> detectConcurrentSessions() {
        List<AnomalyEvent> results = new ArrayList<>();

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLogEntity::getOperation, "LOGIN")
                .ge(OperationLogEntity::getCreateTime, oneHourAgo);

        List<OperationLogEntity> loginLogs = operationLogMapper.selectList(wrapper);

        // Group by user, collect distinct IPs
        Map<Long, Set<String>> userIps = new HashMap<>();
        Map<Long, List<OperationLogEntity>> userLoginLogs = new HashMap<>();

        for (OperationLogEntity logEntry : loginLogs) {
            if (logEntry.getOperatorId() != null && logEntry.getIp() != null) {
                userIps.computeIfAbsent(logEntry.getOperatorId(), k -> new HashSet<>())
                        .add(logEntry.getIp());
                userLoginLogs.computeIfAbsent(logEntry.getOperatorId(), k -> new ArrayList<>())
                        .add(logEntry);
            }
        }

        // Find users with concurrent sessions from multiple IPs
        for (Map.Entry<Long, Set<String>> entry : userIps.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<OperationLogEntity> logs = userLoginLogs.get(entry.getKey());
                List<Long> logIds = logs.stream()
                        .map(OperationLogEntity::getId)
                        .collect(Collectors.toList());
                String username = logs.get(0).getOperatorName();

                results.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("CONCURRENT_SESSION")
                        .severity("HIGH")
                        .description(String.format("User %s logged in from %d different IPs within 1 hour: %s",
                                username, entry.getValue().size(), String.join(", ", entry.getValue())))
                        .userId(entry.getKey())
                        .username(username)
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(logIds)
                        .recommendation("Verify legitimate concurrent access. If unexpected, terminate suspicious sessions and reset password.")
                        .build());
            }
        }

        return results;
    }

    /**
     * Detect HIGH_FREQUENCY_DELETE: more than 5 delete operations by same user in 1 hour.
     */
    private void detectHighFrequencyDelete() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLogEntity::getOperation, "DELETE")
                .ge(OperationLogEntity::getCreateTime, oneHourAgo);

        List<OperationLogEntity> deleteLogs = operationLogMapper.selectList(wrapper);

        Map<Long, List<OperationLogEntity>> byUser = deleteLogs.stream()
                .filter(e -> e.getOperatorId() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getOperatorId));

        for (Map.Entry<Long, List<OperationLogEntity>> entry : byUser.entrySet()) {
            if (entry.getValue().size() > HIGH_FREQUENCY_DELETE_THRESHOLD) {
                List<Long> logIds = entry.getValue().stream()
                        .map(OperationLogEntity::getId)
                        .collect(Collectors.toList());
                String username = entry.getValue().get(0).getOperatorName();

                detectedAnomalies.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("HIGH_FREQUENCY_DELETE")
                        .severity("HIGH")
                        .description(String.format("User %s performed %d delete operations within 1 hour",
                                username, entry.getValue().size()))
                        .userId(entry.getKey())
                        .username(username)
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(logIds)
                        .recommendation("Review delete operations and verify authorization. Consider temporary access restriction.")
                        .build());
            }
        }
    }

    /**
     * Detect BULK_OUTBOUND: outbound of more than 50 units in single transaction.
     */
    private void detectBulkOutbound() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLogEntity::getModule, "InventoryRecord")
                .eq(OperationLogEntity::getOperation, "INSERT")
                .ge(OperationLogEntity::getCreateTime, oneDayAgo);

        List<OperationLogEntity> outboundLogs = operationLogMapper.selectList(wrapper);

        for (OperationLogEntity logEntry : outboundLogs) {
            String params = logEntry.getRequestParams();
            if (params != null && params.contains("OUTBOUND") && containsLargeQuantity(params)) {
                detectedAnomalies.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("BULK_OUTBOUND")
                        .severity("MEDIUM")
                        .description(String.format("Bulk outbound transaction detected by user %s",
                                logEntry.getOperatorName()))
                        .userId(logEntry.getOperatorId())
                        .username(logEntry.getOperatorName())
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(List.of(logEntry.getId()))
                        .recommendation("Verify the bulk outbound is authorized and properly documented.")
                        .build());
            }
        }
    }

    /**
     * Detect UNAUTHORIZED_ATTEMPT: repeated 403 responses from same IP.
     */
    private void detectUnauthorizedAttempt() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLogEntity::getStatus, 0)
                .like(OperationLogEntity::getErrorMessage, "403")
                .ge(OperationLogEntity::getCreateTime, oneHourAgo);

        List<OperationLogEntity> forbiddenLogs = operationLogMapper.selectList(wrapper);

        Map<String, List<OperationLogEntity>> byIp = forbiddenLogs.stream()
                .filter(e -> e.getIp() != null)
                .collect(Collectors.groupingBy(OperationLogEntity::getIp));

        for (Map.Entry<String, List<OperationLogEntity>> entry : byIp.entrySet()) {
            if (entry.getValue().size() >= UNAUTHORIZED_ATTEMPT_THRESHOLD) {
                List<Long> logIds = entry.getValue().stream()
                        .map(OperationLogEntity::getId)
                        .collect(Collectors.toList());
                OperationLogEntity first = entry.getValue().get(0);

                detectedAnomalies.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("UNAUTHORIZED_ATTEMPT")
                        .severity("CRITICAL")
                        .description(String.format("IP %s made %d unauthorized access attempts within 1 hour",
                                entry.getKey(), entry.getValue().size()))
                        .userId(first.getOperatorId())
                        .username(first.getOperatorName())
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(logIds)
                        .recommendation("Consider blocking the IP address and investigate potential attack.")
                        .build());
            }
        }
    }

    /**
     * Detect ABNORMAL_LOGIN: login from new IP or unusual time (outside 6am-10pm).
     */
    private void detectAbnormalLogin() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLogEntity::getOperation, "LOGIN")
                .ge(OperationLogEntity::getCreateTime, oneDayAgo);

        List<OperationLogEntity> loginLogs = operationLogMapper.selectList(wrapper);

        for (OperationLogEntity logEntry : loginLogs) {
            boolean isAnomaly = false;
            List<String> reasons = new ArrayList<>();

            // Check unusual time
            if (logEntry.getCreateTime() != null) {
                LocalTime loginTime = logEntry.getCreateTime().toLocalTime();
                if (loginTime.isBefore(LOGIN_START) || loginTime.isAfter(LOGIN_END)) {
                    isAnomaly = true;
                    reasons.add("login outside normal hours (6am-10pm)");
                }
            }

            // Check new IP
            if (logEntry.getOperatorId() != null && logEntry.getIp() != null) {
                Set<String> knownIps = knownUserIps.computeIfAbsent(logEntry.getOperatorId(), k -> new HashSet<>());
                if (!knownIps.isEmpty() && !knownIps.contains(logEntry.getIp())) {
                    isAnomaly = true;
                    reasons.add("login from new IP: " + logEntry.getIp());
                }
                knownIps.add(logEntry.getIp());
            }

            if (isAnomaly) {
                detectedAnomalies.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("ABNORMAL_LOGIN_TIME")
                        .severity("MEDIUM")
                        .description(String.format("User %s: %s",
                                logEntry.getOperatorName(), String.join("; ", reasons)))
                        .userId(logEntry.getOperatorId())
                        .username(logEntry.getOperatorName())
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(List.of(logEntry.getId()))
                        .recommendation("Verify the login was legitimate. Consider enabling MFA for this account.")
                        .build());
            }
        }
    }

    /**
     * Detect PRIVILEGE_ESCALATION: user modifying their own role/permissions.
     */
    private void detectPrivilegeEscalation() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OperationLogEntity::getModule, "SysRole", "SysMenu")
                .eq(OperationLogEntity::getOperation, "UPDATE")
                .ge(OperationLogEntity::getCreateTime, oneDayAgo);

        List<OperationLogEntity> roleLogs = operationLogMapper.selectList(wrapper);

        for (OperationLogEntity logEntry : roleLogs) {
            String params = logEntry.getRequestParams();
            if (params != null && logEntry.getOperatorId() != null
                    && params.contains(logEntry.getOperatorId().toString())) {
                detectedAnomalies.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("PRIVILEGE_ESCALATION")
                        .severity("CRITICAL")
                        .description(String.format("User %s modified their own role/permissions",
                                logEntry.getOperatorName()))
                        .userId(logEntry.getOperatorId())
                        .username(logEntry.getOperatorName())
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(List.of(logEntry.getId()))
                        .recommendation("Investigate immediately. Self-modification of permissions is a security violation.")
                        .build());
            }
        }
    }

    /**
     * Detect CONCURRENT_SESSION: same user from multiple IPs within 1 hour.
     */
    private void detectConcurrentSession() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLogEntity::getOperation, "LOGIN")
                .ge(OperationLogEntity::getCreateTime, oneHourAgo);

        List<OperationLogEntity> loginLogs = operationLogMapper.selectList(wrapper);

        // Group by user, collect distinct IPs
        Map<Long, Set<String>> userIps = new HashMap<>();
        Map<Long, List<OperationLogEntity>> userLoginLogs = new HashMap<>();

        for (OperationLogEntity logEntry : loginLogs) {
            if (logEntry.getOperatorId() != null && logEntry.getIp() != null) {
                userIps.computeIfAbsent(logEntry.getOperatorId(), k -> new HashSet<>())
                        .add(logEntry.getIp());
                userLoginLogs.computeIfAbsent(logEntry.getOperatorId(), k -> new ArrayList<>())
                        .add(logEntry);
            }
        }

        for (Map.Entry<Long, Set<String>> entry : userIps.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<OperationLogEntity> logs = userLoginLogs.get(entry.getKey());
                List<Long> logIds = logs.stream()
                        .map(OperationLogEntity::getId)
                        .collect(Collectors.toList());
                String username = logs.get(0).getOperatorName();

                detectedAnomalies.add(AnomalyEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .anomalyType("CONCURRENT_SESSION")
                        .severity("HIGH")
                        .description(String.format("User %s logged in from %d different IPs within 1 hour: %s",
                                username, entry.getValue().size(), String.join(", ", entry.getValue())))
                        .userId(entry.getKey())
                        .username(username)
                        .detectedAt(LocalDateTime.now())
                        .relatedLogIds(logIds)
                        .recommendation("Verify legitimate concurrent access. If unexpected, terminate suspicious sessions and reset password.")
                        .build());
            }
        }
    }

    /**
     * Check if request parameters contain a quantity value exceeding the threshold.
     */
    private boolean containsLargeQuantity(String params) {
        try {
            // Simple heuristic: look for quantity patterns in the JSON params
            String lower = params.toLowerCase();
            return lower.contains("\"quantity\"") && extractQuantity(lower) > BULK_OUTBOUND_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    private int extractQuantity(String params) {
        int idx = params.indexOf("\"quantity\"");
        if (idx < 0) return 0;
        String sub = params.substring(idx);
        int colonIdx = sub.indexOf(':');
        if (colonIdx < 0) return 0;
        String afterColon = sub.substring(colonIdx + 1).trim();
        StringBuilder num = new StringBuilder();
        for (char c : afterColon.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            }
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : 0;
    }
}
