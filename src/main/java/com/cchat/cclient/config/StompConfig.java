package com.cchat.cclient.config;

import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        stomp.setMessageConverter(converter);

        stomp.setDefaultHeartbeat(new long[] { 10_000, 10_000 });
        stomp.setTaskScheduler(new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(1)));
        return stomp;
    }
}