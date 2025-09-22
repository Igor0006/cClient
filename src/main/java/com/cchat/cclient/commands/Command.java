package com.cchat.cclient.commands;

public interface Command {
    String name();
    String description();
    void execute(String[] args) throws Exception;
}
