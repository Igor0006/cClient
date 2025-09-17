package com.cchat.cclient;

import java.lang.reflect.Type;
import java.time.Instant;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WsListener {
    private final CliProperties props;
    private final WebSocketStompClient stomp;
    private StompSession session;

    public void start() throws Exception {
        if (session != null && session.isConnected()) return;

        if (!(stomp.getMessageConverter() instanceof MappingJackson2MessageConverter)) {
            stomp.setMessageConverter(new MappingJackson2MessageConverter());
        }

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (props.getJwt() != null && !props.getJwt().isBlank()) {
            headers.add("Authorization", "Bearer " + props.getJwt());
        }
        if (props.getWs().getOrigin() != null) {
            headers.add("Origin", props.getWs().getOrigin());
        }
        headers.add("Sec-WebSocket-Protocol", "v12.stomp");

        session = stomp.connect(props.getWs().getUrl(), headers, new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession s, StompHeaders ch) {
                System.out.println("WS connected. Subscribing to " + props.getWs().getDest());
                s.subscribe(props.getWs().getDest(), new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders headers) { return MessageDto.class; }
                    @Override public void handleFrame(StompHeaders headers, Object payload) {
                        MessageDto msg = (MessageDto) payload;
                        System.out.printf("[%s] WS %s%n", Instant.now(), msg);
                    }
                });
            }
            @Override public void handleTransportError(StompSession s, Throwable ex) {
                System.err.println("WS transport error: " + ex.getMessage());
            }
            @Override public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                System.err.println("WS STOMP error: " + ex.getMessage());
            }
        }).get();
    }
}
