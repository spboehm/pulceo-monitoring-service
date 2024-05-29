package dev.pulceo.pms.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // outgoing
        config.enableSimpleBroker("/orchestrations");

        // TODO: remove, not necessary anymore
        // incoming
        config.setApplicationDestinationPrefixes("/pms");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO: configure AllowedOrigins properly
        // where the websocket connection is finally established
        registry.addEndpoint("/ws").setAllowedOrigins("*");
    }
}