package com.smartscheduler.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Defines the prefix for destinations handled by the message broker (e.g., for subscriptions)
        config.enableSimpleBroker("/topic");

        // Defines the prefix for destinations handled by @MessageMapping in the controllers (e.g., for sending messages)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registers the WebSocket endpoint the client connects to (SockJS('/demo/ws'))
        registry.addEndpoint("/ws")
                // Allows connections from any origin
                .setAllowedOriginPatterns("*")
                .withSockJS()
                // Explicitly set the client library URL to correctly resolve paths
                // when the application is deployed with a context path like '/demo'.
                .setClientLibraryUrl("/demo/js/sockjs.min.js");
    }
}