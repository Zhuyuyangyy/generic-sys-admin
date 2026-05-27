package com.zyy.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket推送服务。
 * <p>通过STOMP向订阅端推送设备状态、耗材告警、操作日志实时消息。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOPIC_EQUIPMENT = "/topic/equipment";
    private static final String TOPIC_CONSUMABLE = "/topic/consumable";
    private static final String TOPIC_OPERATION = "/topic/operation";

    /**
     * 推送设备状态变更。
     *
     * @param equipmentId 设备ID
     * @param status      最新状态（如 "RUNNING", "FAULT", "MAINTENANCE"）
     */
    public void pushEquipmentStatus(String equipmentId, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "EQUIPMENT_STATUS");
        payload.put("equipmentId", equipmentId);
        payload.put("status", status);
        payload.put("timestamp", LocalDateTime.now().toString());

        send(TOPIC_EQUIPMENT + "/" + equipmentId, payload);
    }

    /**
     * 推送耗材库存告警。
     *
     * @param consumableId 耗材ID
     * @param remaining   剩余数量
     */
    public void pushConsumableAlert(String consumableId, Integer remaining) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CONSUMABLE_ALERT");
        payload.put("consumableId", consumableId);
        payload.put("remaining", remaining);
        payload.put("timestamp", LocalDateTime.now().toString());

        String level = (remaining != null && remaining < 10) ? "HIGH" : "NORMAL";
        payload.put("alertLevel", level);

        send(TOPIC_CONSUMABLE + "/" + consumableId, payload);
    }

    /**
     * 推送操作日志（实时）。
     *
     * @param operator 操作人用户名
     * @param action   操作描述
     */
    public void pushOperationLog(String operator, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "OPERATION_LOG");
        payload.put("operator", operator);
        payload.put("action", action);
        payload.put("timestamp", LocalDateTime.now().toString());

        send(TOPIC_OPERATION, payload);
    }

    private void send(String destination, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("WebSocket推送成功 → {} : {}", destination, payload);
        } catch (Exception e) {
            log.warn("WebSocket推送失败 → {} : {}", destination, e.getMessage());
        }
    }
}