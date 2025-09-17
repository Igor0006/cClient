package com.cchat.cclient;

import lombok.Data;

@Data
public class MessageDto {
    Long id;
    Long conversationId;
    Long senderId;
    String body;
}