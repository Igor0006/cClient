package com.cchat.cclient.commands;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.management.InvalidAttributeValueException;

import org.springframework.stereotype.Component;

import com.cchat.cclient.AuthService;
import com.cchat.cclient.CliProperties;
import com.cchat.cclient.ClientState;
import com.cchat.cclient.MessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendCommand implements Command {
    private final ObjectMapper om;
    private final CliProperties props;
    private final AuthService auth;
    private final ClientState clientState;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override public String name() {return "/send";}

    @Override public String description() {return "/send - send message in currently opened chat";}

    @Override
    public void execute(String[] args) throws Exception {
        if (clientState.getCurrentConversationId() < 0) {
            throw new InvalidAttributeValueException();
        }
        MessageDto dto = new MessageDto();
        dto.setBody(String.join(" ", args));
        dto.setSenderId(auth.extractUserIdFromJwt());
        dto.setDestinationId(clientState.getCurrentConversationId());

        String json = om.writeValueAsString(dto);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getIngress() + "/send"))
                .timeout(Duration.ofSeconds(1))
                .header("Authorization", "Bearer " + auth.getJwt())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed: " + resp.statusCode() + " " + resp.body());
        }
        log.info(resp.toString());
    }
    
}

