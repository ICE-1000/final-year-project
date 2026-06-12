package com.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocketMessageBroker

public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (origins.length == 1 && "*".equals(origins[0])) {
            registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
        } else if (origins.length > 0) {
            registry.addEndpoint("/ws").setAllowedOrigins(origins).withSockJS();
        } else {
            registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
        }
    }
}
