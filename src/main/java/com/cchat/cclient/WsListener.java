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

import com.cchat.cclient.commands.ConversationsCommand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WsListener {
    private final CliProperties props;
    private final WebSocketStompClient stomp;
    private final ConversationsCommand convCommand;

    private StompSession session;

    public void start(String jwt) throws Exception {
        if (session != null && session.isConnected()) {
            return;
        }

        if (!(stomp.getMessageConverter() instanceof MappingJackson2MessageConverter)) {
            stomp.setMessageConverter(new MappingJackson2MessageConverter());
        }

        String url = props.getWs().getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("client.ws.url is not configured");
        }

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();

        if (jwt != null && !jwt.isBlank()) {
            headers.add("Authorization", "Bearer " + jwt);
            connectHeaders.add("Authorization", "Bearer " + jwt);
        }

        if (props.getWs().getOrigin() != null) {
            headers.add("Origin", props.getWs().getOrigin());
        }
        headers.add("Sec-WebSocket-Protocol", "v12.stomp");

        session = stomp.connect(url, headers, connectHeaders, new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession s, StompHeaders ch) {
                String configuredDest = props.getWs().getDest();
                if (configuredDest == null || configuredDest.isBlank()) {
                    throw new IllegalStateException("client.ws.dest is not configured");
                }

                for (String dest : resolveDestinations(configuredDest)) {
                    System.out.println("Subscribing to STOMP destination " + dest);
                    s.subscribe(dest, new StompFrameHandler() {
                        @Override public Type getPayloadType(StompHeaders headers) { return MessageDto.class; }
                        @Override public void handleFrame(StompHeaders headers, Object payload) {
                            MessageDto msg = (MessageDto) payload;
                            System.out.printf("[%s] WS %s%n", Instant.now(), msg);
                            convCommand.execute(null);
                        }
                    });
                }
            }
            @Override public void handleTransportError(StompSession s, Throwable ex) {
                System.err.println("WS transport error: " + ex.getMessage());
            }
            @Override public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                System.err.println("WS STOMP error: " + ex.getMessage());
            }
        }).get();
    }

    private java.util.List<String> resolveDestinations(String configured) {
        String normalized = configured.trim();
        if (normalized.isEmpty()) {
            return java.util.List.of();
        }

        if (normalized.startsWith("/user/")) {
            return java.util.List.of(normalized);
        }

        String withoutSlashes = normalized.replaceFirst("^/+", "");
        String userVariant = "/user/" + withoutSlashes;

        if (normalized.equals(userVariant)) {
            return java.util.List.of(normalized);
        }

        if (normalized.startsWith("/")) {
            return java.util.List.of(normalized, userVariant);
        }

        return java.util.List.of("/" + normalized, userVariant);
    }
}
