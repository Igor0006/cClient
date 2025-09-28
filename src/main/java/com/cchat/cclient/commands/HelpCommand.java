package com.cchat.cclient.commands;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Component
public class HelpCommand implements Command{
    @Autowired
    @Lazy
    private CommandRegistry registry;    
    @Override public String name() {return "/help";}

    @Override public String description() {return "/help - get description of all commands";}

    @Override
    public void execute(String[] args) throws Exception {
        for (var command: registry.all()) {
            System.out.println(command.description());
        }
    }
    
}
