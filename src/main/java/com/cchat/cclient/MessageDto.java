package com.cchat.cclient;

import lombok.Data;

@Data
public class MessageDto {
    Long id;
    Long conversation_id;
    Long sender_id;
    String body;
}