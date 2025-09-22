package com.cchat.cclient.commands;

import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cchat.cclient.AuthService;
import com.cchat.cclient.CliProperties;
import com.cchat.cclient.ClientState;
import com.cchat.cclient.MessageDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SendCommand implements Command {
        private final ObjectMapper om;
    private final CliProperties props;
    private final AuthService auth;
    private final ClientState clientState;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override public String name() {return "/send";}

    @Override public String description() {return "Send message in current conversation.";}

    @Override
    public void execute(String[] args) throws Exception{
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
        System.out.println(resp);
    }
    
}

