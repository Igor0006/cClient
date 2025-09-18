package com.cchat.cclient.commands;


import org.springframework.stereotype.Component;

@Component
public class ExitCommand implements Command {
    @Override public String name()  { return "/exit"; }
    @Override public String description() { return "/exit — exit from the app"; }

    @Override
    public void execute(String[] args) {
        System.out.println("Bye.");
        System.exit(0);
    }
}