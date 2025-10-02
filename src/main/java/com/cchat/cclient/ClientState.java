package com.cchat.cclient;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.cchat.cclient.commands.ConversationsCommand;
import com.cchat.cclient.model.ConversationResetEvent;
import com.cchat.cclient.model.ConversationSelectedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientState {
    private final WsListener listener;
    private final ConversationsCommand convCommand;
    
    private Long selfId;
    private Long currentConversationId = -1L;

    public Long getCurrentConversationId() {
        return currentConversationId;
    }
    
    @EventListener
    public void setCurrentConversationId(ConversationSelectedEvent e) {
        this.currentConversationId = e.conversationId();
        listener.subMessages(currentConversationId);
    }
    
    @EventListener
    public void resetCurrentConversation(ConversationResetEvent e) {
        listener.subListUpdates();
        convCommand.getChats();
        currentConversationId = -1L;
    }
}
