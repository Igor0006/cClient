package com.cchat.cclient.commands;


import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CommandRegistry {
    private final Map<String, Command> byName = new HashMap<>();

    public CommandRegistry(List<Command> commands) {
        for (Command c : commands) {
            byName.put(c.name(), c);
        }
    }

    public Optional<Command> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Collection<Command> all() { return byName.values(); }
}