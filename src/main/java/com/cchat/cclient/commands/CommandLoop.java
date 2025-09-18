package com.cchat.cclient.commands;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class CommandLoop {
    private final CommandRegistry registry;

    public void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            String line = sc.nextLine().trim();
            if (line.isBlank()) continue;

            String[] parts = line.split(" ");
            String cmdName = parts[0];
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);

            registry.find(cmdName).ifPresentOrElse(cmd -> {
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }, () -> System.out.println("Unknown command. Use /help"));
        }
    }
}