package com.cchat.cclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix="client")
@Data
public class CliProperties {
    private String jwt;
    private Ws ws = new Ws();
    private Api api = new Api();

    @Data
    public static class Ws {
        private String url;
        private String dest;
        private String origin;
    }

    @Data
    public static class Api {
        private String ingress;
        private String receive;
    }
}
