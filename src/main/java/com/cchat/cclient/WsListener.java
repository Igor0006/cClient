package com.cchat.cclient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.cchat.cclient.model.ConversationResetEvent;
import com.cchat.cclient.model.MessageDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WsListener {

    private final CliProperties props;
    private final WebSocketStompClient stomp;
    private final ApplicationEventPublisher events;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger attempt = new AtomicInteger(0);
    private volatile boolean reconnectScheduled = false;
    private volatile boolean shouldStayConnected = false;
    private volatile String lastJwt;

    private volatile StompSession session;
    private final List<StompSession.Subscription> subs = new ArrayList<>();

    private enum Mode {
        NONE, LIST_UPDATES, MESSAGES
    }

    private volatile Mode lastMode = Mode.NONE;
    private volatile Long lastConversationId;

    public synchronized void start(String jwt) {
        if (session != null && session.isConnected())
            return;

        if (!(stomp.getMessageConverter() instanceof MappingJackson2MessageConverter)) {
            stomp.setMessageConverter(new MappingJackson2MessageConverter());
        }
        if (stomp.getTaskScheduler() == null) {
            stomp.setTaskScheduler(new ConcurrentTaskScheduler(scheduler));
        }
        stomp.setDefaultHeartbeat(new long[] { 10_000, 10_000 }); // 10s/10s

        shouldStayConnected = true;
        lastJwt = jwt;
        attempt.set(0);
        connectOnce();
    }

    public synchronized void subListUpdates() {
        lastMode = Mode.LIST_UPDATES;
        lastConversationId = null;
        if (isConnected()) {
            resubscribeListUpdates();
        }
    }

    public synchronized void subMessages(Long conversationId) {
        lastMode = Mode.MESSAGES;
        lastConversationId = conversationId;
        if (isConnected()) {
            resubscribeMessages(conversationId);
        }
    }

    private boolean isConnected() {
        return session != null && session.isConnected();
    }

    private void connectOnce() {
        final String url = props.getWs().getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("client.ws.url is not configured");
        }

        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        if (lastJwt != null && !lastJwt.isBlank()) {
            wsHeaders.add("Authorization", "Bearer " + lastJwt);
            connectHeaders.add("Authorization", "Bearer " + lastJwt);
        }
        String origin = props.getWs().getOrigin();
        if (origin != null && !origin.isBlank()) {
            wsHeaders.add("Origin", origin);
        }
        wsHeaders.add("Sec-WebSocket-Protocol", "v12.stomp");

        log.info("WS connecting to {}", url);

        stomp.connectAsync(url, wsHeaders, connectHeaders, new SessionHandler());
    }

    private synchronized void onConnected(StompSession newSession) {
        this.session = newSession;
        attempt.set(0);
        cancelReconnect();
        log.info("WS connected. Session id={}", newSession.getSessionId());

        switch (lastMode) {
            case LIST_UPDATES -> resubscribeListUpdates();
            case MESSAGES -> {
                if (lastConversationId != null)
                    resubscribeMessages(lastConversationId);
            }
            default -> {
            }
        }
    }

    private synchronized void scheduleReconnect(Throwable cause) {
        if (!shouldStayConnected)
            return;
        if (reconnectScheduled)
            return;

        int n = attempt.getAndIncrement();
        long delayMillis = backoffMillis(n);
        reconnectScheduled = true;

        log.warn("WS will try to reconnect in {} ms (attempt #{}). Cause: {}",
                delayMillis, n + 1, cause != null ? cause.getMessage() : "unknown");

        scheduler.schedule(() -> {
            reconnectScheduled = false;
            if (shouldStayConnected)
                connectOnce();
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelReconnect() {
        reconnectScheduled = false;
    }

    private long backoffMillis(int attempt) {
        long base = 1_000L << Math.min(attempt, 5); // up to 32s
        long capped = Math.min(base, 30_000L);
        long jitter = (long) (Math.random() * 500L);
        return capped + jitter;
    }

    private synchronized void cleanSubs() {
        for (var s : subs) {
            try {
                s.unsubscribe();
            } catch (Throwable ignored) {
            }
        }
        subs.clear();
    }

    private synchronized void resubscribeListUpdates() {
        if (!isConnected())
            return;
        cleanSubs();
        var sub = session.subscribe("/user/queue/update", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("WS subListUpdates event.");
                events.publishEvent(new ConversationResetEvent());
            }
        });
        subs.add(sub);
        log.info("Listening all chats");
    }

    private synchronized void resubscribeMessages(Long conversationId) {
        if (!isConnected())
            return;
        Objects.requireNonNull(conversationId, "conversationId");
        cleanSubs();
        var sub = session.subscribe("/user/queue/messages" + conversationId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                MessageDto msg = (MessageDto) payload;
                System.out.printf("[%s] WS %s%n", Instant.now(), msg);
                try {
                    session.send("/chat/" + conversationId + "/read", null);
                } catch (Throwable ex) {
                    log.warn("WS send(read) failed: {}", ex.getMessage());
                }
            }
        });
        subs.add(sub);
        log.info("Listening current chat messages of id: {}", conversationId);
    }

    private class SessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            onConnected(session);
        }

        @Override
        public void handleTransportError(StompSession s, Throwable ex) {
            log.error("WS transport error: {}", ex.getMessage());
            scheduleReconnect(ex);
        }

        @Override
        public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
            log.error("WS STOMP error: {}", ex.getMessage());
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            log.warn("WS unexpected frame: {}", headers);
        }

    }
}