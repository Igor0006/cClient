package com.cchat.cclient;

import lombok.Data;

@Data
public class MessageDto {
    private String body;
    private Long senderId;
    private Long destinationId;
    private String type;
}