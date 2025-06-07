package com.phaser.tank;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TankWebSocketHandler(), "/ws/create")
                .setAllowedOrigins("*");
        registry.addHandler(new TankWebSocketHandler(), "/ws/join/{roomId}")
                .setAllowedOrigins("*");
    }
}
