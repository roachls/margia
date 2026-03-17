package org.roach.margia.command;

/**
 * The contract for a command that can be undone or redone
 */
public interface Command {
    /**
     * @param undo {@code true} if this is an undo
     */
    void execute(boolean undo);

    /**
     * execute the command without saving it for undo
     */
    default void execute() {
        execute(false);
    }
}
