package com.zyy.websocket;

import com.zyy.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RESTful controller for event center operations.
 * <p>
 * API Design:
 * <ul>
 *   <li>GET /api/events/recent         - Get recent events (last 50)</li>
 *   <li>GET /api/events/subscribe-info  - Get WebSocket connection info</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Event Center", description = "Real-time event center and WebSocket connection info")
public class EventController {

    @GetMapping("/recent")
    @Operation(summary = "Recent events", description = "Get recent events from in-memory buffer (last 50)")
    public Result<List<Event>> getRecentEvents() {
        List<Event> events = EventWebSocket.getRecentEvents();
        return Result.ok(events);
    }

    @GetMapping("/subscribe-info")
    @Operation(summary = "WebSocket info", description = "Get WebSocket connection information")
    public Result<Map<String, Object>> getSubscribeInfo() {
        Map<String, Object> info = EventWebSocket.getConnectionInfo();
        return Result.ok(info);
    }
}
