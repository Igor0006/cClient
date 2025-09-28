package com.cchat.cclient.commands;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Component
@RequiredArgsConstructor
@Slf4j
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
                    log.info("Error " + e.getMessage());
                    System.out.println("Wrong command args or usage. Use /help");
                }
            }, () -> System.out.println("Unknown command. Use /help"));
        }
    }
}