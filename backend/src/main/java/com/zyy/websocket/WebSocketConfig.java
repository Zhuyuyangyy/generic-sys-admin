package com.zyy.websocket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

    /**
     * 仅在容器真的提供了 ServerContainer 时注册。
     *
     * ServerEndpointExporter 依赖 jakarta.websocket.server.ServerContainer，该 bean
     * 只由内嵌 Servlet 容器的 WebServer 初始化阶段产生。@SpringBootTest 默认的
     * 上下文类型虽然是 SERVLET，但没有走 WebServer 初始化，于是该 bean 缺失，
     * 无条件注册会抛 "ServerContainer not available" 让整个容器启动失败。
     */
    @Bean
    @ConditionalOnBean(name = "serverContainer")
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
