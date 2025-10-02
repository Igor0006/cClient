package com.cchat.cclient.commands;


import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.net.URI;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.cchat.cclient.model.Clean;
import com.cchat.cclient.model.ConversationResetEvent;
import com.cchat.cclient.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ConversationsCommand implements Command {
    private final ApplicationEventPublisher events;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper om;
    private final com.cchat.cclient.config.CliProperties props;
    private final AuthService auth;

    @Override public String name()  { return "/conversations"; }
    @Override public String description() { return "/conversations - show your conversations"; }

    public record ConversationDto(Long id, String title, boolean unread) {}

    public List<ConversationDto> list() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getReceive() + "/conversations"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + auth.getJwt())
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed: " + resp.statusCode() + " " + resp.body());
        }
        return om.readValue(resp.body(), new TypeReference<List<ConversationDto>>() {});
    }
    
    @Clean
    public void getChats() {
        try {
            var list = list();
            if (list.isEmpty()) {
                System.out.println("no conversations yet");
                return;
            }
            list.forEach(c -> System.out.printf("(%d) %s %s%n",
                    c.id(), c.title(), c.unread() ? "[unread]" : ""));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    @Override
    public void execute(String[] args) {
        events.publishEvent(new ConversationResetEvent());
    }
}
