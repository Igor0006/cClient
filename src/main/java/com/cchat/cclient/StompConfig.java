package com.cchat.cclient;

import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Configuration
public class StompConfig {

    @Bean
    public WebSocketStompClient stompClient() {
        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        stomp.setDefaultHeartbeat(new long[]{10_000, 10_000});
        stomp.setTaskScheduler(new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(1)));
        return stomp;
    }
}