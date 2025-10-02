package com.cchat.cclient.services;

import com.cchat.cclient.config.CliProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper om;
    private final CliProperties props;

    private String jwt;

    public boolean isAuthenticated() {
        return jwt != null && !jwt.isBlank();
    }
    
    public String getJwt() { return jwt; }

    public JwtResponse register(String username, String password) throws Exception {
        return executeAuthRequest("/register", new Request(username, password), "Register failed");
    }

    public JwtResponse login(String username, String password) throws Exception {
        return executeAuthRequest("/login", new Request(username, password), "Login failed");
    }

    public boolean pingMe() throws Exception {
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getReceive() + "/me"))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200;
    }

    private JwtResponse executeAuthRequest(String path, Object payload, String errorPrefix) throws Exception {
        String body = om.writeValueAsString(payload);
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(props.getApi().getIngress() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException(errorPrefix + " (" + resp.statusCode() + "): " + resp.body());
        }
        JwtResponse jwtResp = om.readValue(resp.body(), JwtResponse.class);
        jwt = jwtResp.getToken();
        return jwtResp;
    }
    
    public Long extractUserIdFromJwt() {
        try {
            String[] parts = jwt.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payload = om.readTree(payloadJson);
            if (!payload.has("uid"))
                throw new IllegalStateException("JWT has no uid");
            return payload.get("uid").asLong();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JWT payload", e);
        }
    }

    @Data
    private static final class Request {
        private final String username;
        private final String password;
    }

    @Data
    public static final class JwtResponse {
        @com.fasterxml.jackson.annotation.JsonAlias({"access", "token"})
        private String token;
    }
}