package org.roach.margia.rules;

import java.util.List;
import java.util.Map;

import org.roach.margia.*;
import org.roach.margia.actions.PlayNote;
import org.roach.margia.messages.MusicianMessage;

import static org.roach.margia.Note.*;

/**
 * each musician has an assigned sequence that it plays and then passes along a
 * message to start
 */
public class SequenceRule extends AbstractMusicianRule {
    // @formatter:off
    private final Map<Integer, List<MusicianMessage>> seqMap = Map.of(
            0, List.of(new NoteInfo(C2, 64, 4), new NoteInfo(G2, 100, 4), new NoteInfo(DS2, 64, 6),
                       new StartSequenceMessage(), new NoteInfo(D2, 64, 2), new NoteInfo(C2, 64, 2),
                       new NoteInfo(DS2, 64, 2), new NoteInfo(D2, 120, 2), new NoteInfo(C2, 64, 2), 
                       new NoteInfo(B1, 48, 2), new NoteInfo(D2, 52, 2), new NoteInfo(G1, 56, 2),
                       new NoteInfo(Note.REST, 60, 2)),
            1, List.of(new NoteInfo(G1, 64, 4), new NoteInfo(D2, 100, 4), new NoteInfo(AS1, 64, 6),
                    new StartSequenceMessage(),
                    new NoteInfo(A1, 64, 2), new NoteInfo(G1, 64, 2), new NoteInfo(AS1, 64, 2),
                    new NoteInfo(A1, 64, 2), new NoteInfo(G1, 64, 2), new NoteInfo(FS1, 48, 2), 
                    new NoteInfo(A1, 64, 2), new NoteInfo(D1, 64, 2), 
                    new NoteInfo(Note.REST, 0, 2)),
            2, List.of(new NoteInfo(D2, 64, 4), new NoteInfo(A2, 100, 4), new NoteInfo(F2, 64, 6),
                    new StartSequenceMessage(),
                    new NoteInfo(E2, 64, 2), new NoteInfo(D2, 64, 2), new NoteInfo(F2, 64, 2),
                    new NoteInfo(E2, 64, 2), new NoteInfo(D2, 64, 2), new NoteInfo(CS2, 48, 2), 
                    new NoteInfo(E2, 64, 2), new NoteInfo(A1, 64, 2),
                    new NoteInfo(Note.REST, 0, 2)),
            3, List.of(new NoteInfo(A1, 64, 4), new NoteInfo(D2, 100, 4), new NoteInfo(C2, 64, 6),
                    new StartSequenceMessage(),
                    new NoteInfo(B1, 64, 2), new NoteInfo(A1, 64, 2), new NoteInfo(C2, 64, 2),
                    new NoteInfo(B1, 64, 2), new NoteInfo(A1, 64, 2), new NoteInfo(GS1, 48, 2), 
                    new NoteInfo(B1, 64, 2), new NoteInfo(E1, 64, 2),
                    new NoteInfo(Note.REST, 0, 2))
    );
    // @formatter:on

    private List<MusicianMessage> sequence;
    private int index;
    private boolean isPlayingSequence;
    private int ticksRemainingInNote;

    @Override
    public void setMusician(Musician musician) {
        super.setMusician(musician);
        this.sequence = seqMap.get(musician.getId() % seqMap.size());
    }

    @Override
    public void calculateAction(long tick) {
        if (ticksRemainingInNote > 1) {
            ticksRemainingInNote--;
            return;
        }
        if (isPlayingSequence && index < sequence.size()) {
            var message = sequence.get(index);
            if (message instanceof NoteInfo note) {
                ticksRemainingInNote = note.length();
                actionsToTake.add(new PlayNote(musician, note));
            } else {
                actionsToTake.add(() -> musician.sendMessageToPeers(message));
                index++;
                calculateAction(tick);
            }
            index++;
            if (index >= sequence.size()) {
                logger.atDebug().log("{} done with sequence");
                isPlayingSequence = false;
            }
            return;
        }
        var message = musician.getNextNoteHeard();
        if (message instanceof StartSequenceMessage) {
            index = 0;
            isPlayingSequence = true;
        }
    }

    /**
     * message to start playing a sequence
     */
    public static class StartSequenceMessage implements MusicianMessage {
        // nothing
    }

    @Override
    public String getName() { return "Sequence"; }

    @Override
    public void reset() {
        index = 0;
        this.sequence = seqMap.get(musician.getId() % seqMap.size());
        isPlayingSequence = false;
        ticksRemainingInNote = 0;
    }

    @Override
    public Map<String, Object> storableProperties() {
        return Map.of(RULE_CLASSNAME_PROPERTY, SequenceRule.class.getName());
    }

    @Override
    public void restoreFromStorage(Map<String, Object> storableProperties) {
        // TODO
    }

}
