package com.cchat.cclient.commands;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.cchat.cclient.AuthService;
import com.cchat.cclient.CliProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddContactCommand implements Command {
    private final CliProperties props;
    private final AuthService auth;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();


    @Override public String name() {return "/addContact";}

    @Override public String description() {
        return "/addCommand {userName} = add user with followed username to your conversations";}

    @Override
    public void execute(String[] args) throws Exception {
        String contact = args[0];

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getReceive() + "/addContact"))
                .timeout(Duration.ofSeconds(1))
                .header("Authorization", "Bearer " + auth.getJwt())
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(contact)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed: " + resp.statusCode() + " " + resp.body());
        }
        log.info(resp.toString());
    }
}
