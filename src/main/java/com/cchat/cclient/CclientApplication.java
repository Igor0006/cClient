package com.cchat.cclient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CclientApplication implements CommandLineRunner {

    private final WsListener wsListener;

    public CclientApplication(WsListener wsListener) {
        this.wsListener = wsListener;
    }

    public static void main(String[] args) {
        SpringApplication.run(CclientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        wsListener.start();
        System.out.println("Listening (WS)...");
        Thread.currentThread().join();
    }
}
