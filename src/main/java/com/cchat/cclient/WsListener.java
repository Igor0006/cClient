package com.cchat.cclient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.cchat.cclient.model.ConversationResetEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WsListener {
    private final CliProperties props;
    private final WebSocketStompClient stomp;
    private final ApplicationEventPublisher events;
    private List<Subscription> subList = new ArrayList<Subscription>();

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
            @Override public void handleTransportError(StompSession s, Throwable ex) {
                log.error("WS transport error: " + ex.getMessage());
            }
            @Override public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
                log.error("WS STOMP error: " + ex.getMessage());
            }
        }).get();
    }

    private void cleanSubs() {
        for (var s: subList) {s.unsubscribe();}
    }

    public void subListUpdates() {
        cleanSubs();
        subList.add(session.subscribe("/topic/ping", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return MessageDto.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                log.info("Message from WS subListUpdates subscription.");
                events.publishEvent(new ConversationResetEvent());
            }
        }));
        log.info("Listening all chats.");
    }

    public void subMessages(Long conversationId) {
        cleanSubs();
        subList.add(session.subscribe("/user/queue/messages" + conversationId, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return MessageDto.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                MessageDto msg = (MessageDto) payload;
                System.out.printf("[%s] WS %s%n", Instant.now(), msg);
            }
        }));
        log.info("Listening current chat messages of id: " + conversationId);
    }
}
