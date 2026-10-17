package com.example.taskmanager.model;

import java.util.EnumSet;
import java.util.Set;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    private static final EnumSet<TaskStatus> TERMINAL = EnumSet.of(DONE, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public static Set<TaskStatus> activeStatuses() {
        return EnumSet.complementOf(TERMINAL);
    }
}
