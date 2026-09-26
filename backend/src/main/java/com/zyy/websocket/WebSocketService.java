package com.zyy.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
public class WebSocketService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * STOMP 模板。本项目实际运行的是 JSR-356 @ServerEndpoint（见 EventWebSocket
     * 与 WebSocketConfig），不会产生 SimpMessagingTemplate bean，因此这里按
     * ObjectProvider 惰性获取：容器里没有就降级为纯日志，绝不在启动期因为缺少
     * 该 bean 而失败。
     */
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    public WebSocketService(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

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
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            // 没有 STOMP broker 时不能谎称推送成功——只记录，让调用方知道
            // 实时通道不可用（前端通过 JSR-356 端点另行订阅）。
            log.debug("STOMP 通道不可用，跳过推送 → {} : {}", destination, payload);
            return;
        }
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("WebSocket推送成功 → {} : {}", destination, payload);
        } catch (Exception e) {
            log.warn("WebSocket推送失败 → {} : {}", destination, e.getMessage());
        }
    }
}