package org.roach.margia.command;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.roach.margia.view.Memento;

/**
 * Stores commands in order to undo/redo them
 */
public class CommandCaretaker {
    private static final Deque<Memento> UNDO_STACK = new ConcurrentLinkedDeque<>();

    private CommandCaretaker() {
        // static methods only
    }

    /**
     * @param memento the memento to push onto the stack
     */
    public static void pushStack(Memento memento) {
        UNDO_STACK.push(memento);
    }

    /**
     * Pop a memento off the stack (if any) and execute its commands
     */
    public static void undo() {
        if (UNDO_STACK.isEmpty())
            return;
        var memento = UNDO_STACK.pop();
        for (var command : memento.undoCommands()) {
            command.execute(true);
        }
    }
}
