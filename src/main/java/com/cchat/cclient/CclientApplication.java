package com.cchat.cclient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.Map;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class CclientApplication {
    public static void main(String[] args) throws Exception {
        final String WS_URL = "ws://localhost:8081/gs-guide-websocket";
        final String DEST ="/topic/messages";

        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        stomp.setDefaultHeartbeat(new long[]{10_000, 10_000});
        stomp.setTaskScheduler(new org.springframework.scheduling.concurrent.ConcurrentTaskScheduler());

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();

        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhYm9iYSIsImlhdCI6MTc1Nzk2MDMzNCwiZXhwIjoxNzU3OTY3NTM0fQ.X9RFUAhJG89VnCzTlXVnT90wFxYLRsg5iT98ZSv9whU";

        if (jwt != null && !jwt.isBlank()) {
            httpHeaders.add("Authorization", "Bearer " + jwt);
        }
        httpHeaders.add("Origin", "http://localhost:8081");
        httpHeaders.add("Sec-WebSocket-Protocol", "v12.stomp");

        System.out.println("Connecting to " + WS_URL + " ...");
        stomp.connect(WS_URL, httpHeaders, new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession s, StompHeaders ch) {
                System.out.println("Connected. Subscribing to " + DEST);
                s.subscribe(DEST, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders headers) { return Map.class; }
                    @Override public void handleFrame(StompHeaders headers, Object payload) {
                        @SuppressWarnings("unchecked") Map<String, Object> msg = (Map<String, Object>) payload;
                        System.out.printf("[%s] %s%n", Instant.now(), msg);
                    }
                });
            }
            @Override public void handleTransportError(StompSession s, Throwable ex) {
                System.err.println("Transport error: " + ex.getMessage());
            }
            @Override public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                System.err.println("STOMP error: " + ex.getMessage());
            }
        }).get(10, TimeUnit.SECONDS);

        System.out.println("Listening...");
        Thread.currentThread().join();
    }
}
