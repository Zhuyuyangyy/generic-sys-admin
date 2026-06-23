package com.zyy.websocket;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Simple POJO representing a WebSocket event payload.
 * <p>
 * Used as the standard envelope for all real-time events
 * pushed through the WebSocket channel.
 *
 * @author System Architect
 */
@Data
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Event type identifier, e.g. STOCK_ALERT, MAINTENANCE_OVERDUE */
    private String type;

    /** Event payload (arbitrary object, serialized to JSON) */
    private Object data;

    /** Event generation timestamp */
    private LocalDateTime timestamp;

    /** Target user ID (nullable — when null the event is broadcast) */
    private Long userId;
}
