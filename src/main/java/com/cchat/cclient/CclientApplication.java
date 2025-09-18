package com.cchat.cclient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.cchat.cclient.commands.CommandLoop;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class CclientApplication implements CommandLineRunner {

    private final WsListener wsListener;
    private final AuthCli authCli;
    private final AuthService authService;
    private final CommandLoop commandLoop;

    public static void main(String[] args) {
        SpringApplication.run(CclientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        authCli.authenticateBlocking();

        wsListener.start(authService.getJwt());

        System.out.println("Listening (WS)...");

        commandLoop.run();
        Thread.currentThread().join();
    }
}
