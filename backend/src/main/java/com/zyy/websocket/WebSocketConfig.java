package com.zyy.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket configuration class.
 * <p>
 * Registers the {@link ServerEndpointExporter} bean to enable
 * JSR-356 (@ServerEndpoint) WebSocket support in the embedded
 * Servlet container.
 *
 * @author System Architect
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
