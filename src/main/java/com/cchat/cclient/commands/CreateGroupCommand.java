package com.cchat.cclient.commands;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.management.InvalidAttributeValueException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.cchat.cclient.config.CliProperties;
import com.cchat.cclient.model.ConversationResetEvent;
import com.cchat.cclient.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateGroupCommand implements Command{
    private final ApplicationEventPublisher events;
    private final CliProperties props;
    private final AuthService auth;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override public String name() {return "/createGroup"; }

    @Override
    public String description() { return "/createGroup <GroupName> <userName1> <userName2> ... - create a group and add users with corresponding names in it"; }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            throw new InvalidAttributeValueException();
        }
        
        List<String> userIds = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            userIds.add(args[i]);
        }

        String body = new ObjectMapper().writeValueAsString(userIds);
        
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getReceive() + "/createGroup/" + args[0]))
                .timeout(Duration.ofSeconds(1))
                .header("Authorization", "Bearer " + auth.getJwt())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed: " + resp.statusCode() + " " + resp.body());
        }
        log.info("Group created: {}", resp.toString());
        events.publishEvent(new ConversationResetEvent());
    }
    
}
