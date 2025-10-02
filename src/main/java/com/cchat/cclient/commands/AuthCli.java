package com.cchat.cclient.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.cchat.cclient.AuthService;

import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class AuthCli {
    private final AuthService auth;

    public void authenticateBlocking() {
        Scanner sc = new Scanner(System.in);

        while (!auth.isAuthenticated()) {
            System.out.print("Login or register in the system. \n");
            String line = sc.nextLine().trim();

            if (line.isBlank()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                System.out.println("Wrong command.");
            }
            String cmd = parts[0];

            try {
                switch (cmd) {
                    case "/login":
                        auth.login(parts[1], parts[2]);
                        System.out.println("Logged in. Token saved.");
                        break;
                    case "/register":
                        auth.register(parts[1], parts[2]);
                        System.out.println("Registered. Token saved.");
                        break;
                    case "/exit":
                        System.out.println("Bye.");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Wrong command.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}