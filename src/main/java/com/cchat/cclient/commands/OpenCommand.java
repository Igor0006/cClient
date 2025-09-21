package com.cchat.cclient.commands;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.message.Message;
import org.springframework.stereotype.Component;

import com.cchat.cclient.AuthService;
import com.cchat.cclient.CliProperties;
import com.cchat.cclient.ClientState;
import com.cchat.cclient.MessageDto;
import com.cchat.cclient.commands.ConversationsCommand.ConversationDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OpenCommand implements Command {
    private final ObjectMapper om;
    private final CliProperties props;
    private final AuthService auth;
    private final ClientState clientState;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override public String name() {return "/open";}

    @Override public String description() {return "/open {conversation_id} - open conversation"; }

    public List<MessageDto> list(String arg) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getReceive() + "/chat/" + arg))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + auth.getJwt())
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed: " + resp.statusCode() + " " + resp.body());
        }
        return om.readValue(resp.body(), new TypeReference<List<MessageDto>>() {});
    }

    @Override
    public void execute(String[] args) throws RuntimeException {
        try {
            var list  = list(args[0]);
            clientState.setCurrentConversationId(Long.valueOf(args[0]));
            list.forEach(c ->
                System.out.println(c));   
        } catch (Exception e) {
            System.out.println("Wrong input " + e.getMessage());
        }
    }
    
}
