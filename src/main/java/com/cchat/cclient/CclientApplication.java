package com.cchat.cclient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.cchat.cclient.commands.CommandLoop;
import com.cchat.cclient.commands.ConversationsCommand;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class CclientApplication implements CommandLineRunner {

    private final WsListener wsListener;
    private final AuthCli authCli;
    private final AuthService authService;
    private final CommandLoop commandLoop;
    private final ConversationsCommand convCommand;

    public static void main(String[] args) {
        SpringApplication.run(CclientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        authCli.authenticateBlocking();

        wsListener.start(authService.getJwt());
        wsListener.subListUpdates();
        // wsListener.subMessages(3L);
        
        convCommand.execute(null);
        commandLoop.run();
        Thread.currentThread().join();
    }
}
