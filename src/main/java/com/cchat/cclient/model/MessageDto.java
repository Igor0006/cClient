package com.cchat.cclient.model;

import java.time.Instant;

import lombok.Data;

@Data
public class MessageDto {
    private String body;
    private Long sender_id;
    private Long conversation_id;
    private Instant createdAt;
}