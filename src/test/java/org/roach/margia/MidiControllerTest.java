package org.roach.margia;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MidiControllerTest {

    @Test
    @Disabled("Only for local use with a DAW")
    @SuppressWarnings({ "java:S2699", "java:S2925" })
    void testPlayNote() throws InterruptedException {
        MidiController.init(MidiController.LOOP_MIDI, 30);
        var controller = MidiController.getInstance();
        for (var note : Key.DRUMPAD.notes()) {
            System.out.println("Playing " + note);
            controller.playNote(7, new NoteInfo(note, 70, 1));
            Thread.sleep(1000);
        }
        controller.close();
    }

}
