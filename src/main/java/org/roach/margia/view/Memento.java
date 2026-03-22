package org.roach.margia.view;

import java.util.List;

import org.roach.margia.command.Command;

/**
 * Stores commands needs to implement undo/redo functionality
 * 
 * @param redoCommand  the last command that was executed
 * @param undoCommands the commands to undo the redoCommand
 * 
 */
public record Memento(Command redoCommand, List<Command> undoCommands) {

}
