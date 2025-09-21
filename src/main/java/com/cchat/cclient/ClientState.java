package com.cchat.cclient;

import org.springframework.stereotype.Component;

@Component
public class ClientState {
    private Long currentConversationId = -1L;

    public Long getCurrentConversationId() {
        return currentConversationId;
    }

    public void setCurrentConversationId(Long currentConversationId) {
        this.currentConversationId = currentConversationId;
    }

    public void resetCurrentConversation() {
        currentConversationId = -1L;
    }
    
}
